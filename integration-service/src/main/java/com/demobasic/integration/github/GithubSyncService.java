package com.demobasic.integration.github;

import com.demobasic.integration.domain.SyncStateService;
import com.demobasic.integration.event.ProjectEvent;
import com.demobasic.integration.rabbit.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class GithubSyncService {

    private static final Logger log = LoggerFactory.getLogger(GithubSyncService.class);
    private static final String ROUTING_KEY = "github.commit.sync";

    private final GithubProperties properties;
    private final EventPublisher publisher;
    private final SyncStateService syncState;
    private final WebClient.Builder webClientBuilder;

    public GithubSyncService(GithubProperties properties,
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
            log.warn("GitHub not configured (GITHUB_TOKEN/GITHUB_OWNER/GITHUB_REPO); skipping");
            return 0;
        }

        WebClient client = webClientBuilder.baseUrl("https://api.github.com").build();
        JsonNode body;
        try {
            body = client.get()
                    .uri(uri -> {
                        var builder = uri.path("/repos/{owner}/{repo}/commits")
                                .queryParam("per_page", properties.getPerPage());
                        if (properties.getBranch() != null && !properties.getBranch().isBlank()) {
                            builder = builder.queryParam("sha", properties.getBranch());
                        }
                        return builder.build(properties.getOwner(), properties.getRepo());
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ex) {
            log.error("GitHub fetch failed: {}", ex.getMessage());
            return 0;
        }
        if (body == null || !body.isArray()) {
            log.warn("GitHub response is not an array");
            return 0;
        }

        String lastSha = syncState.get(SyncStateService.KEY_GITHUB_LAST_SHA).orElse("");
        List<JsonNode> newCommits = new ArrayList<>();
        Iterator<JsonNode> it = body.elements();
        while (it.hasNext()) {
            JsonNode commit = it.next();
            String sha = commit.path("sha").asText();
            if (sha.equals(lastSha)) {
                break;
            }
            newCommits.add(commit);
        }

        Collections.reverse(newCommits);

        int published = 0;
        String newestSha = lastSha;
        for (JsonNode commit : newCommits) {
            String sha = commit.path("sha").asText();
            String message = commit.path("commit").path("message").asText("");
            String title = message.split("\\r?\\n", 2)[0];
            String htmlUrl = commit.path("html_url").asText(null);

            ProjectEvent event = new ProjectEvent(
                    ProjectEvent.SOURCE_GITHUB,
                    "CommitPushed",
                    sha,
                    title,
                    message,
                    null,
                    htmlUrl,
                    OffsetDateTime.now()
            );
            publisher.publish(ROUTING_KEY, event);
            newestSha = sha;
            published++;
        }

        if (!newestSha.equals(lastSha)) {
            syncState.put(SyncStateService.KEY_GITHUB_LAST_SHA, newestSha);
        }
        log.info("GitHub sync published {} commit(s); newest sha={}", published, newestSha);
        return published;
    }
}
