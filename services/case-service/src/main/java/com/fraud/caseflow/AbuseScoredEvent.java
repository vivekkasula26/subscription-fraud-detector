package com.fraud.caseflow;

import java.time.Instant;
import java.util.List;

public record AbuseScoredEvent(
        String accountId,
        String subscriptionId,
        int score,
        RiskBand band,
        List<Reason> reasons,
        Instant scoredAt) {

    public AbuseScoredEvent {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscriptionId must not be blank");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (band == null || expectedBand(score) != band) {
            throw new IllegalArgumentException("band must match score thresholds");
        }
        if (reasons == null || reasons.stream().anyMatch(reason -> reason == null)) {
            throw new IllegalArgumentException("reasons must not be null or contain null values");
        }
        reasons = List.copyOf(reasons);
        if (scoredAt == null) {
            throw new IllegalArgumentException("scoredAt must not be null");
        }
    }

    private static RiskBand expectedBand(int score) {
        if (score >= 80) {
            return RiskBand.HIGH;
        }
        if (score >= 40) {
            return RiskBand.MEDIUM;
        }
        return RiskBand.LOW;
    }

    public record Reason(String ruleId, int weight, String detail) {
        public Reason {
            if (ruleId == null || ruleId.isBlank()) {
                throw new IllegalArgumentException("reason ruleId must not be blank");
            }
            if (weight < 0 || weight > 100) {
                throw new IllegalArgumentException("reason weight must be between 0 and 100");
            }
            if (detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("reason detail must not be blank");
            }
        }
    }
}
