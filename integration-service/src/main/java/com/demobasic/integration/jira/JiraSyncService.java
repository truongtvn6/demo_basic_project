package com.demobasic.integration.jira;

import com.demobasic.integration.domain.SyncStateService;
import com.demobasic.integration.event.ProjectEvent;
import com.demobasic.integration.rabbit.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Iterator;

@Service
public class JiraSyncService {

    private static final Logger log = LoggerFactory.getLogger(JiraSyncService.class);
    private static final String ROUTING_KEY = "jira.issue.updated";

    private final JiraProperties properties;
    private final EventPublisher publisher;
    private final SyncStateService syncState;
    private final WebClient.Builder webClientBuilder;

    public JiraSyncService(JiraProperties properties,
                           EventPublisher publisher,
                           SyncStateService syncState,
                           WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.publisher = publisher;
        this.syncState = syncState;
        this.webClientBuilder = webClientBuilder;
    }

    public int sync() {
        if (!properties.isConfigured()) {
            log.warn("Jira not configured (JIRA_BASE_URL/JIRA_USER_EMAIL/JIRA_API_TOKEN/JIRA_PROJECT_KEY); skipping");
            return 0;
        }

        String basic = Base64.getEncoder().encodeToString(
                (properties.getEmail() + ":" + properties.getApiToken()).getBytes(StandardCharsets.UTF_8));
        String jql = "project = " + properties.getProjectKey() + " ORDER BY updated DESC";

        WebClient client = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
        JsonNode body;
        try {
            body = client.get()
                    .uri(uri -> uri.path("/rest/api/3/search")
                            .queryParam("jql", jql)
                            .queryParam("maxResults", properties.getMaxResults())
                            .queryParam("fields", "summary,status,updated")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ex) {
            log.error("Jira fetch failed: {}", ex.getMessage());
            return 0;
        }
        if (body == null || !body.has("issues")) {
            log.warn("Jira response has no issues field");
            return 0;
        }

        String lastSyncAt = syncState.get(SyncStateService.KEY_JIRA_LAST_SYNC_AT).orElse("");
        String newestUpdated = lastSyncAt;
        int published = 0;

        Iterator<JsonNode> issues = body.get("issues").elements();
        while (issues.hasNext()) {
            JsonNode issue = issues.next();
            String key = issue.path("key").asText();
            JsonNode fields = issue.path("fields");
            String summary = fields.path("summary").asText(null);
            String status = fields.path("status").path("name").asText(null);
            String updated = fields.path("updated").asText(null);
            String browseUrl = properties.getBaseUrl().replaceAll("/+$", "") + "/browse/" + key;

            if (updated != null && updated.compareTo(lastSyncAt) <= 0) {
                continue;
            }
            if (updated != null && updated.compareTo(newestUpdated) > 0) {
                newestUpdated = updated;
            }

            ProjectEvent event = new ProjectEvent(
                    ProjectEvent.SOURCE_JIRA,
                    "IssueUpdated",
                    key,
                    summary,
                    "Jira issue updated",
                    status,
                    browseUrl,
                    OffsetDateTime.now()
            );
            publisher.publish(ROUTING_KEY, event);
            published++;
        }

        if (!newestUpdated.equals(lastSyncAt)) {
            syncState.put(SyncStateService.KEY_JIRA_LAST_SYNC_AT, newestUpdated);
        }
        log.info("Jira sync published {} event(s); newest updated={}", published, newestUpdated);
        return published;
    }
}
