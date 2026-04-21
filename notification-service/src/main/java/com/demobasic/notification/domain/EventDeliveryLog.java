package com.demobasic.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_delivery_log")
public class EventDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false, length = 128)
    private String routingKey;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "external_id", nullable = false, length = 256)
    private String externalId;

    @Column(length = 512)
    private String title;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    protected EventDeliveryLog() {
    }

    public EventDeliveryLog(String routingKey, String source, String externalId,
                            String title, boolean success, String errorMessage) {
        this.routingKey = routingKey;
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.deliveredAt = OffsetDateTime.now();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
