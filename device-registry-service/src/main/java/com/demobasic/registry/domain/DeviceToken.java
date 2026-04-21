package com.demobasic.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(length = 128)
    private String label;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DeviceToken() {
    }

    public DeviceToken(String token, String label) {
        this.token = token;
        this.label = label;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getLabel() {
        return label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
