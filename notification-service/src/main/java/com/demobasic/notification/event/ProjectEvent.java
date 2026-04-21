package com.demobasic.notification.event;

import java.time.OffsetDateTime;

public record ProjectEvent(
        String source,
        String eventType,
        String externalId,
        String title,
        String detail,
        String status,
        String url,
        OffsetDateTime occurredAt
) {
}
