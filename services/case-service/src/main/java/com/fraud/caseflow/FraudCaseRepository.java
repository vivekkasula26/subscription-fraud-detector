package com.fraud.caseflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID>, JpaSpecificationExecutor<FraudCase> {
    Optional<FraudCase> findByAccountIdAndSubscriptionId(String accountId, String subscriptionId);
}
