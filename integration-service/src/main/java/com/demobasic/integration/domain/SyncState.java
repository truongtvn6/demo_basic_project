package com.demobasic.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_state")
public class SyncState {

    @Id
    @Column(length = 64)
    private String key;

    @Column(length = 512)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SyncState() {
    }

    public SyncState(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateValue(String newValue) {
        this.value = newValue;
        this.updatedAt = OffsetDateTime.now();
    }
}
