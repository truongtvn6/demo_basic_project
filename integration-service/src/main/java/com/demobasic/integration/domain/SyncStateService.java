package com.demobasic.integration.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SyncStateService {

    public static final String KEY_JIRA_LAST_SYNC_AT = "JIRA_LAST_SYNC_AT";
    public static final String KEY_GITHUB_LAST_SHA = "GITHUB_LAST_SHA";

    private final SyncStateRepository repository;

    public SyncStateService(SyncStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        return repository.findById(key).map(SyncState::getValue);
    }

    @Transactional
    public void put(String key, String value) {
        repository.findById(key)
                .map(existing -> {
                    existing.updateValue(value);
                    return existing;
                })
                .orElseGet(() -> repository.save(new SyncState(key, value)));
    }
}
