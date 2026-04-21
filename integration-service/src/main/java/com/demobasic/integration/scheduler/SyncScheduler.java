package com.demobasic.integration.scheduler;

import com.demobasic.integration.github.GithubSyncService;
import com.demobasic.integration.jira.JiraSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "demobasic.scheduler", name = "enabled", havingValue = "true")
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final JiraSyncService jira;
    private final GithubSyncService github;

    public SyncScheduler(JiraSyncService jira, GithubSyncService github) {
        this.jira = jira;
        this.github = github;
    }

    @Scheduled(fixedDelayString = "${demobasic.scheduler.interval-ms:60000}",
               initialDelayString = "${demobasic.scheduler.interval-ms:60000}")
    public void run() {
        log.info("Scheduled sync tick");
        jira.sync();
        github.sync();
    }
}
