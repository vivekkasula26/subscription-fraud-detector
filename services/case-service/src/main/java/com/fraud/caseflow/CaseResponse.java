package com.fraud.caseflow;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String accountId,
        String subscriptionId,
        int riskScore,
        RiskBand band,
        CaseStatus status,
        JsonNode reasons,
        Instant createdAt) {

    public static CaseResponse from(FraudCase fraudCase) {
        return new CaseResponse(
                fraudCase.getId(),
                fraudCase.getAccountId(),
                fraudCase.getSubscriptionId(),
                fraudCase.getRiskScore(),
                fraudCase.getBand(),
                fraudCase.getStatus(),
                fraudCase.getReasons(),
                fraudCase.getCreatedAt());
    }
}
