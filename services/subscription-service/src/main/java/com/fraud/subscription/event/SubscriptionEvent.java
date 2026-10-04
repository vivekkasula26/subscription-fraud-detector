package com.fraud.subscription.event;

/** Published to {@code subscription.events}; eventType is "start" or "cancel". */
public record SubscriptionEvent(
        String accountId,
        String subscriptionId,
        String eventType,
        String timestamp) {
}
