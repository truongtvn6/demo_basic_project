package com.demobasic.integration.api;

import com.demobasic.integration.event.ProjectEvent;
import com.demobasic.integration.github.GithubSyncService;
import com.demobasic.integration.jira.JiraSyncService;
import com.demobasic.integration.rabbit.EventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sync")
public class SyncController {

    private final JiraSyncService jira;
    private final GithubSyncService github;
    private final EventPublisher publisher;

    public SyncController(JiraSyncService jira, GithubSyncService github, EventPublisher publisher) {
        this.jira = jira;
        this.github = github;
        this.publisher = publisher;
    }

    @PostMapping
    public Map<String, Integer> syncAll() {
        int jiraCount = jira.sync();
        int ghCount = github.sync();
        return Map.of("jira", jiraCount, "github", ghCount);
    }

    @PostMapping("/jira")
    public Map<String, Integer> syncJira() {
        return Map.of("jira", jira.sync());
    }

    @PostMapping("/github")
    public Map<String, Integer> syncGithub() {
        return Map.of("github", github.sync());
    }

    @PostMapping("/mock")
    public Map<String, String> mock() {
        String id = UUID.randomUUID().toString();
        ProjectEvent event = new ProjectEvent(
                ProjectEvent.SOURCE_MOCK,
                "TaskUpdated",
                id,
                "Mock event " + id.substring(0, 8),
                "Triggered via /sync/mock",
                "InProgress",
                null,
                OffsetDateTime.now()
        );
        publisher.publish("demo.mock.tick", event);
        return Map.of("status", "published", "externalId", id);
    }
}
