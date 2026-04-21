package com.demobasic.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDeliveryLogRepository extends JpaRepository<EventDeliveryLog, Long> {
    boolean existsBySourceAndExternalId(String source, String externalId);
}
