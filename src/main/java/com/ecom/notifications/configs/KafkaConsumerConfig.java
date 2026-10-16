package com.ecom.notifications.configs;

import com.ecom.notifications.exceptions.InvalidEventException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {
    public static final String DLT_SUFFIX = ".DLT";

    /**
     * Failed events:
     * - invalid (bad JSON, unknown type, missing fields, unknown user): never retried, sent to "<topic>.DLT"
     * - anything else (e.g. User Service down during the email lookup): retried after 1, 2, 4, 8, 16 and 32 s
     *   (about a minute), then sent to "<topic>.DLT" for inspection and replay
     * The offset is committed after the record is handled or dead-lettered, so nothing is skipped silently.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(1000, 2.0);
        backOff.setMaxElapsedTime(63_000);
        // Explicit "<topic>.DLT" on the same partition. Spring Kafka 3.3's default is "<topic>-dlt"; the broker has
        // auto-create off, so a name mismatch would make every dead-lettering fail and block the partition.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class, JsonProcessingException.class,
                DeserializationException.class);
        return handler;
    }

    // Dead-letter topics, same partition count as the source topics (the recoverer keeps the partition)
    @Bean
    public NewTopic userEventsDlt(@Value("${events.topics.user}") String topic) {
        return dlt(topic);
    }

    @Bean
    public NewTopic orderEventsDlt(@Value("${events.topics.order}") String topic) {
        return dlt(topic);
    }

    @Bean
    public NewTopic paymentEventsDlt(@Value("${events.topics.payment}") String topic) {
        return dlt(topic);
    }

    private static NewTopic dlt(String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(3).replicas(1).build();
    }
}
