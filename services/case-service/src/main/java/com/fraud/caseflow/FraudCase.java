package com.fraud.caseflow;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_cases")
public class FraudCase {
    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(name = "subscription_id", nullable = false, length = 128)
    private String subscriptionId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskBand band;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaseStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode reasons;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudCase() {
    }

    public FraudCase(AbuseScoredEvent event, JsonNode reasons) {
        this.id = UUID.randomUUID();
        this.accountId = event.accountId();
        this.subscriptionId = event.subscriptionId();
        this.riskScore = event.score();
        this.band = event.band();
        this.status = CaseStatus.OPEN;
        this.reasons = reasons;
        this.createdAt = event.scoredAt();
    }

    public UUID getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskBand getBand() {
        return band;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public JsonNode getReasons() {
        return reasons;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
