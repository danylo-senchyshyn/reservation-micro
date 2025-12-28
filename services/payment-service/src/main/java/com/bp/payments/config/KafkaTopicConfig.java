package com.bp.payments.config;

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

    @Value("${app.kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${app.kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    /**
     * Payment confirmed topic new topic.
     *
     * @return the new topic
     */
    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name(paymentConfirmedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Payment failed topic new topic.
     *
     * @return the new topic
     */
    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
