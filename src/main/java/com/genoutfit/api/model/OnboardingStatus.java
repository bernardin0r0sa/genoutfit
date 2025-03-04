package com.genoutfit.api.model;

public enum OnboardingStatus {
    NEW,                // Just signed up
    PLAN_SELECTED,      // User has selected a subscription plan
    PROFILE_COMPLETED,  // Basic profile info provided
    PAYMENT_PENDING,    // Shown preview, ready for payment
    COMPLETED           // Paid and ready to use
}