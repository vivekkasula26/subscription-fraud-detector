package com.fraud.subscription.dto;

import com.fraud.subscription.domain.SubscriptionStatus;

public record CancelResponse(SubscriptionStatus status) {
}
