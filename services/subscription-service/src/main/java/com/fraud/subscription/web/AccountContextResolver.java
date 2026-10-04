package com.fraud.subscription.web;

import com.fraud.subscription.service.SubscriptionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reads the caller identity from headers the gateway injects after validating the JWT
 * (X-Account-Id, X-Device-Fingerprint, X-Client-Ip). To switch to an internal call to the
 * account service, replace this class only.
 */
@Component
public class AccountContextResolver {

    static final String ACCOUNT_ID = "X-Account-Id";
    static final String DEVICE_FINGERPRINT = "X-Device-Fingerprint";
    static final String CLIENT_IP = "X-Client-Ip";

    public AccountContext resolve(HttpServletRequest request) {
        String accountId = request.getHeader(ACCOUNT_ID);
        if (accountId == null || accountId.isBlank()) {
            throw new SubscriptionException(HttpStatus.UNAUTHORIZED, "Missing " + ACCOUNT_ID);
        }
        String ip = request.getHeader(CLIENT_IP);
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return new AccountContext(accountId, request.getHeader(DEVICE_FINGERPRINT), ip);
    }
}
