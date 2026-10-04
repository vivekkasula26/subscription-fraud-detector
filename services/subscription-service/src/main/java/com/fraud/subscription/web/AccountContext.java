package com.fraud.subscription.web;

/** Caller identity resolved from the request. */
public record AccountContext(String accountId, String deviceFingerprint, String ip) {
}
