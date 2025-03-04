package com.genoutfit.api.model;

public enum SubscriptionPlan {
    TRIAL("Trial", 5, "One-time purchase"),           // One-time 5 outfits
    BASIC("Basic", 100, "Monthly"),                   // $19/month, 100 outfits
    PREMIUM("Premium", 350, "Monthly");               // $49/month, 350 outfits

    private final String displayName;
    private final int monthlyOutfitQuota;
    private final String billingCycle;

    SubscriptionPlan(String displayName, int monthlyOutfitQuota, String billingCycle) {
        this.displayName = displayName;
        this.monthlyOutfitQuota = monthlyOutfitQuota;
        this.billingCycle = billingCycle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMonthlyOutfitQuota() {
        return monthlyOutfitQuota;
    }

    public String getBillingCycle() {
        return billingCycle;
    }
}