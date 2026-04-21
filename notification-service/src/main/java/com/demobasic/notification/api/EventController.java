package com.demobasic.notification.api;

import com.demobasic.notification.domain.EventDeliveryLog;
import com.demobasic.notification.domain.EventDeliveryLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventDeliveryLogRepository repository;

    public EventController(EventDeliveryLogRepository repository) {
        this.repository = repository;
    }

    public record EventView(
            Long id,
            String source,
            String routingKey,
            String externalId,
            String title,
            OffsetDateTime deliveredAt,
            boolean success,
            String errorMessage
    ) {
        static EventView from(EventDeliveryLog e) {
            return new EventView(
                    e.getId(),
                    e.getSource(),
                    e.getRoutingKey(),
                    e.getExternalId(),
                    e.getTitle(),
                    e.getDeliveredAt(),
                    e.isSuccess(),
                    e.getErrorMessage()
            );
        }
    }

    @GetMapping("/recent")
    public List<EventView> recent(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        int bounded = Math.min(Math.max(limit, 1), 200);
        return repository.findAll(
                        PageRequest.of(0, bounded, Sort.by(Sort.Direction.DESC, "deliveredAt")))
                .getContent()
                .stream()
                .map(EventView::from)
                .toList();
    }
}
