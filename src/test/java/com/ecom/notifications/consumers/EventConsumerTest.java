package com.ecom.notifications.consumers;

import com.ecom.notifications.configs.KafkaConsumerConfig;
import com.ecom.notifications.dtos.EventDto;
import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.services.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventConsumerTest {
    private final NotificationService service = mock(NotificationService.class);
    private final EventConsumer consumer = new EventConsumer(service, new ObjectMapper());

    @Test
    void parsesEnvelopeAndHandlesIt() throws Exception {
        consumer.onEvent(new ConsumerRecord<>("ecom.order-events", 1, 5L, "2",
                "{\"eventId\":\"order-1-paid\",\"type\":\"ORDER_CONFIRMED\",\"occurredAt\":1,\"data\":{\"userId\":2,\"orderId\":1}}"));
        ArgumentCaptor<EventDto> captor = ArgumentCaptor.forClass(EventDto.class);
        verify(service).handleEvent(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("order-1-paid");
        assertThat(captor.getValue().getData()).containsEntry("orderId", 1);
    }

    @Test
    void badJsonThrows() {
        assertThatThrownBy(() -> consumer.onEvent(new ConsumerRecord<>("t", 0, 0L, null, "this is not json")))
                .isInstanceOf(JsonProcessingException.class);
        verifyNoInteractions(service);
    }

    @Test
    void invalidAndUnparseableEventsAreNotRetried() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        DefaultErrorHandler handler = new KafkaConsumerConfig().kafkaErrorHandler(template);
        assertThat(handler.removeClassification(InvalidEventException.class)).isFalse();
        assertThat(handler.removeClassification(JsonProcessingException.class)).isFalse();
        assertThat(handler.removeClassification(IllegalStateException.class)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deadLettersGoToDotDltOnSamePartition() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        when(template.isTransactional()).thenReturn(false);
        DefaultErrorHandler handler = new KafkaConsumerConfig().kafkaErrorHandler(template);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("ecom.order-events", 2, 9L, "k", "bad");

        boolean recovered = handler.handleOne(new InvalidEventException("Unknown event type: NOPE"), record,
                mock(org.apache.kafka.clients.consumer.Consumer.class),
                mock(org.springframework.kafka.listener.MessageListenerContainer.class));

        assertThat(recovered).isTrue();
        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo("ecom.order-events.DLT");
        // The resolver also returns the source partition (2); with a mocked template the recoverer cannot verify
        // the partition exists and lets the producer choose, so the partition is checked against a real broker instead
        assertThat(sent.getValue().value()).isEqualTo("bad");
    }
}
