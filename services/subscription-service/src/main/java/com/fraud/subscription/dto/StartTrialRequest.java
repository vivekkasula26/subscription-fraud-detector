package com.fraud.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Raw card data. Must never be logged or persisted. */
public record StartTrialRequest(
        @NotBlank @Pattern(regexp = "[0-9 -]{12,23}", message = "invalid card number") String cardNumber,
        @NotBlank @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}", message = "expiry must be MM/YY") String expiry,
        @NotBlank @Pattern(regexp = "[0-9]{3,4}", message = "invalid cvv") String cvv) {

    @Override
    public String toString() {
        return "StartTrialRequest[REDACTED]";
    }
}
