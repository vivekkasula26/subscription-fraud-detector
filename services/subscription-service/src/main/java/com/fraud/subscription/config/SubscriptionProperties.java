package com.fraud.subscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "subscription")
public record SubscriptionProperties(
        int trialDays,
        String cardFingerprintSecret,
        Topics topics) {

    public record Topics(String trialStarted, String subscriptionEvents) {
    }
}
