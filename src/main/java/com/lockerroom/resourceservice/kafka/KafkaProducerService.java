package com.lockerroom.resourceservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, String key, Object event) {
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
