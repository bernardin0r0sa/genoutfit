package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Subscription entity to track user subscription details
@Entity
@Table(name = "user_subscriptions")
@Data
@NoArgsConstructor
public class UserSubscription {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private SubscriptionPlan plan;

    @Column(name = "remaining_outfits")
    private int remainingOutfits;

    @Column(name = "subscription_start")
    private LocalDateTime subscriptionStart;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "active")
    private boolean active = true;

    // Constructor for subscriptions
    public UserSubscription(String userId, SubscriptionPlan plan, String stripeSubscriptionId, String stripeCustomerId) {
        this.userId = userId;
        this.plan = plan;
        this.remainingOutfits = plan.getMonthlyOutfitQuota();
        this.subscriptionStart = LocalDateTime.now();

        // Set next billing date based on plan
        if (plan == SubscriptionPlan.TRIAL) {
            this.nextBillingDate = null; // No next billing for trial
        } else {
            this.nextBillingDate = this.subscriptionStart.plusMonths(1);
        }

        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeCustomerId = stripeCustomerId;
    }

    // Method to check if user can generate an outfit
    public boolean canGenerateOutfit() {
        return active && remainingOutfits > 0;
    }

    // Method to decrement outfit count
    public void useOutfit() {
        if (remainingOutfits > 0) {
            remainingOutfits--;
        }
    }

    // Method to reset outfit quota (for monthly renewals)
    public void resetMonthlyQuota() {
        remainingOutfits = plan.getMonthlyOutfitQuota();
        nextBillingDate = LocalDateTime.now().plusMonths(1);
    }
}