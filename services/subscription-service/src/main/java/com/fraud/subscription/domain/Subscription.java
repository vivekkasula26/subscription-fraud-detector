package com.fraud.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    private String id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "trial_started_at", nullable = false)
    private Instant trialStartedAt;

    @Column(name = "trial_ends_at", nullable = false)
    private Instant trialEndsAt;

    /** Irreversible card hash. The raw card number is never stored. */
    @Column(name = "card_fingerprint", nullable = false)
    private String cardFingerprint;

    protected Subscription() {
    }

    public Subscription(String id, String accountId, Instant trialStartedAt,
                        Instant trialEndsAt, String cardFingerprint) {
        this.id = id;
        this.accountId = accountId;
        this.status = SubscriptionStatus.TRIALING;
        this.trialStartedAt = trialStartedAt;
        this.trialEndsAt = trialEndsAt;
        this.cardFingerprint = cardFingerprint;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public SubscriptionStatus getStatus() { return status; }
    public Instant getTrialStartedAt() { return trialStartedAt; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public String getCardFingerprint() { return cardFingerprint; }
}
