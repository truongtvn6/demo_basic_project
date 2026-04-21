package com.demobasic.integration.rabbit;

import com.demobasic.integration.event.ProjectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String routingKey, ProjectEvent event) {
        log.info("Publishing routingKey={} externalId={} source={}", routingKey, event.externalId(), event.source());
        rabbitTemplate.convertAndSend(routingKey, event);
    }
}
