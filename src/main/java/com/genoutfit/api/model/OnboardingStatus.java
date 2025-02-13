package com.genoutfit.api.model;

public enum OnboardingStatus {
    NEW,                // Just signed up
    PROFILE_COMPLETED,  // Basic profile info provided
    PAYMENT_PENDING,    // Shown preview, ready for payment
    COMPLETED          // Paid and ready to use
}