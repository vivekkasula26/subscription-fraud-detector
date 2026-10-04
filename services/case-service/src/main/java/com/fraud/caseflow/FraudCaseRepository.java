package com.fraud.caseflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {
    Optional<FraudCase> findByAccountIdAndSubscriptionId(String accountId, String subscriptionId);
}
