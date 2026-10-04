package com.fraud.subscription.repository;

import com.fraud.subscription.domain.Subscription;
import com.fraud.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    boolean existsByAccountIdAndStatus(String accountId, SubscriptionStatus status);
}
