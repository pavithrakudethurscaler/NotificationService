package com.ecom.notifications.consumers;

import com.ecom.notifications.dtos.EventDto;
import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.services.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the User, Order and Payment event topics (HLD 3.6). Each message is the same envelope the REST
 * intake accepts, handled by the same NotificationService.handleEvent.
 * <p>
 * Errors propagate to the container's error handler (KafkaConsumerConfig): invalid events go straight to the
 * topic's .DLT, other failures are retried with backoff first. Duplicate deliveries are dropped by eventId.
 */
@Component
public class EventConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public EventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"${events.topics.user}", "${events.topics.order}", "${events.topics.payment}"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void onEvent(ConsumerRecord<String, String> record) throws JsonProcessingException, InvalidEventException {
        EventDto event = objectMapper.readValue(record.value(), EventDto.class);
        boolean created = notificationService.handleEvent(event);
        log.info("Event {} ({}) from {}-{}@{}: {}", event.getEventId(), event.getType(), record.topic(),
                record.partition(), record.offset(), created ? "notification created" : "duplicate ignored");
    }
}
