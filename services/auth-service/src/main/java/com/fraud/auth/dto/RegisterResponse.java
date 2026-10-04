package com.fraud.auth.dto;

import com.fraud.auth.model.AccountStatus;
import java.time.Instant;

public record RegisterResponse(
        Long accountId,
        String firstName,
        String lastName,
        String email,
        AccountStatus status,
        Instant createdAt
) {}
