package com.fraud.auth.dto;

public record ErrorResponse(
        int status,
        String message
) {}
