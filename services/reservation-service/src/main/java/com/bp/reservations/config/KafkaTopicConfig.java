package com.bp.reservations.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The type Kafka topic config.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.reservation-created}")
    private String reservationCreatedTopic;

    /**
     * Reservation created topic new topic.
     *
     * @return the new topic
     */
    @Bean
    public NewTopic reservationCreatedTopic() {
        return TopicBuilder.name(reservationCreatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}