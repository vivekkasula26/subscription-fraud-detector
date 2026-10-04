package com.fraud.subscription.event;

import com.fraud.subscription.config.SubscriptionProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Keyed by accountId so all events for an account stay ordered on one partition. */
@Component
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, Object> kafka;
    private final SubscriptionProperties props;

    public SubscriptionEventPublisher(KafkaTemplate<String, Object> kafka, SubscriptionProperties props) {
        this.kafka = kafka;
        this.props = props;
    }

    public void publishTrialStarted(TrialStartedEvent event) {
        kafka.send(props.topics().trialStarted(), event.accountId(), event);
    }

    public void publishSubscriptionEvent(SubscriptionEvent event) {
        kafka.send(props.topics().subscriptionEvents(), event.accountId(), event);
    }
}
