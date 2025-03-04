package com.genoutfit.api.repository;

import com.genoutfit.api.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Repository for the above entity
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
    List<UserSubscription> findByNextBillingDateBefore(LocalDateTime date);
    List<UserSubscription> findByActive(boolean active);
}