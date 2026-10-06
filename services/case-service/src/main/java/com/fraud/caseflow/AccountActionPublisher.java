package com.fraud.caseflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AccountActionPublisher {
    private final KafkaTemplate<String, AccountActionEvent> kafkaTemplate;
    private final String topic;

    public AccountActionPublisher(
            KafkaTemplate<String, AccountActionEvent> kafkaTemplate,
            @Value("${app.kafka.topics.account-actions}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(AccountActionEvent event) {
        try {
            kafkaTemplate.send(topic, event.accountId(), event).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing account action", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Failed to publish account action", exception);
        }
    }
}
