package com.fraud.subscription.event;

/** Published to {@code trial.started}; the enriched event Abuse Scoring reacts to. */
public record TrialStartedEvent(
        String accountId,
        String subscriptionId,
        String cardFingerprint,
        String deviceFingerprint,
        String ip,
        String timestamp) {
}
