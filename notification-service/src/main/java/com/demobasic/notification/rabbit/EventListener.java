package com.demobasic.notification.rabbit;

import com.demobasic.notification.domain.EventDeliveryLog;
import com.demobasic.notification.domain.EventDeliveryLogRepository;
import com.demobasic.notification.event.ProjectEvent;
import com.demobasic.notification.fcm.FcmSender;
import com.demobasic.notification.registry.TokenClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final TokenClient tokenClient;
    private final FcmSender fcmSender;
    private final EventDeliveryLogRepository logRepository;

    public EventListener(TokenClient tokenClient,
                         FcmSender fcmSender,
                         EventDeliveryLogRepository logRepository) {
        this.tokenClient = tokenClient;
        this.fcmSender = fcmSender;
        this.logRepository = logRepository;
    }

    @RabbitListener(queues = "${demobasic.rabbit.queue}")
    @Transactional
    public void onMessage(ProjectEvent event) {
        log.info("Received event source={} externalId={} title={}",
                event.source(), event.externalId(), event.title());

        if (event.externalId() != null
                && logRepository.existsBySourceAndExternalId(event.source(), event.externalId())) {
            log.info("Duplicate event source={} externalId={}; skipping",
                    event.source(), event.externalId());
            return;
        }

        String title = "[" + event.source() + "] " + (event.title() == null ? event.eventType() : event.title());
        String body = event.detail() == null ? event.eventType() : event.detail();

        Map<String, String> data = new HashMap<>();
        if (event.source() != null) data.put("source", event.source());
        if (event.eventType() != null) data.put("eventType", event.eventType());
        if (event.externalId() != null) data.put("externalId", event.externalId());
        if (event.url() != null) data.put("url", event.url());
        if (event.status() != null) data.put("status", event.status());

        List<String> tokens = tokenClient.fetchTokens();
        FcmSender.FcmResult result = fcmSender.send(tokens, title, body, data);

        String routingKey = deriveRoutingKey(event.source());
        logRepository.save(new EventDeliveryLog(
                routingKey,
                event.source(),
                event.externalId() == null ? "null" : event.externalId(),
                event.title(),
                result.errorMessage() == null,
                result.errorMessage()
        ));
    }

    private String deriveRoutingKey(String source) {
        if (source == null) return "unknown";
        return switch (source) {
            case "JIRA" -> "jira.issue.updated";
            case "GITHUB" -> "github.commit.sync";
            case "MOCK" -> "demo.mock.tick";
            default -> source.toLowerCase();
        };
    }
}
