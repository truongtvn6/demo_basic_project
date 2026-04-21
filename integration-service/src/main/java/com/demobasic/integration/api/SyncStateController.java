package com.demobasic.integration.api;

import com.demobasic.integration.domain.SyncStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/sync")
public class SyncStateController {

    private final SyncStateService syncState;

    public SyncStateController(SyncStateService syncState) {
        this.syncState = syncState;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jiraLastSyncAt",
                syncState.get(SyncStateService.KEY_JIRA_LAST_SYNC_AT).orElse(null));
        body.put("githubLastSha",
                syncState.get(SyncStateService.KEY_GITHUB_LAST_SHA).orElse(null));
        return body;
    }
}
