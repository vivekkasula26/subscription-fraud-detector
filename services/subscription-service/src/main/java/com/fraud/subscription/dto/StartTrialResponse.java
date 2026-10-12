package com.fraud.subscription.dto;

import java.time.Instant;

public record StartTrialResponse(String subscriptionId, Instant trialEndsAt) {
}
