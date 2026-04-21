package com.demobasic.integration.event;

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
    public static final String SOURCE_MOCK = "MOCK";
    public static final String SOURCE_JIRA = "JIRA";
    public static final String SOURCE_GITHUB = "GITHUB";
}
