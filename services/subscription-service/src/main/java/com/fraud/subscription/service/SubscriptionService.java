package com.fraud.subscription.service;

import com.fraud.subscription.config.SubscriptionProperties;
import com.fraud.subscription.domain.Subscription;
import com.fraud.subscription.domain.SubscriptionStatus;
import com.fraud.subscription.dto.CancelResponse;
import com.fraud.subscription.dto.StartTrialRequest;
import com.fraud.subscription.dto.StartTrialResponse;
import com.fraud.subscription.event.SubscriptionEvent;
import com.fraud.subscription.event.SubscriptionEventPublisher;
import com.fraud.subscription.event.TrialStartedEvent;
import com.fraud.subscription.repository.SubscriptionRepository;
import com.fraud.subscription.web.AccountContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final CardFingerprinter fingerprinter;
    private final SubscriptionEventPublisher publisher;
    private final SubscriptionProperties props;

    public SubscriptionService(SubscriptionRepository repository, CardFingerprinter fingerprinter,
                               SubscriptionEventPublisher publisher, SubscriptionProperties props) {
        this.repository = repository;
        this.fingerprinter = fingerprinter;
        this.publisher = publisher;
        this.props = props;
    }

    public StartTrialResponse startTrial(AccountContext account, StartTrialRequest request) {
        if (repository.existsByAccountIdAndStatus(account.accountId(), SubscriptionStatus.TRIALING)) {
            throw new SubscriptionException(HttpStatus.CONFLICT, "Account already has an active trial");
        }

        Instant now = Instant.now();
        String cardFingerprint = fingerprinter.fingerprint(request.cardNumber());
        Subscription subscription = repository.save(new Subscription(
                "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                account.accountId(),
                now,
                now.plus(Duration.ofDays(props.trialDays())),
                cardFingerprint));

        publisher.publishTrialStarted(new TrialStartedEvent(
                account.accountId(), subscription.getId(), cardFingerprint,
                account.deviceFingerprint(), account.ip(), now.toString()));
        publisher.publishSubscriptionEvent(new SubscriptionEvent(
                account.accountId(), subscription.getId(), "start", now.toString()));

        return new StartTrialResponse(subscription.getId(), subscription.getTrialEndsAt());
    }

    public CancelResponse cancel(AccountContext account, String subscriptionId) {
        Subscription subscription = repository.findById(subscriptionId)
                .filter(s -> s.getAccountId().equals(account.accountId()))
                .orElseThrow(() -> new SubscriptionException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (subscription.getStatus() != SubscriptionStatus.TRIALING) {
            throw new SubscriptionException(HttpStatus.CONFLICT, "Subscription is not an active trial");
        }

        subscription.cancel();
        repository.save(subscription);
        publisher.publishSubscriptionEvent(new SubscriptionEvent(
                account.accountId(), subscription.getId(), "cancel", Instant.now().toString()));

        return new CancelResponse(subscription.getStatus());
    }
}
