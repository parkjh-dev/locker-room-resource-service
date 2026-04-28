package com.lockerroom.resourceservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    public void send(String topic, String key, Object event) {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate not available — skipping event publish to topic: {}", topic);
            return;
        }

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send Kafka event to topic {}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Kafka event sent to topic {} with key {}", topic, key);
                    }
                });
    }
}
