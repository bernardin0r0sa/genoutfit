package com.genoutfit.api.service;

import com.genoutfit.api.model.SubscriptionPlan;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserSubscription;
import com.genoutfit.api.repository.UserRepository;
import com.genoutfit.api.repository.UserSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StripeService {
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.price.trial}")
    private String stripeTrial;

    @Value("${stripe.price.basic}")
    private String stripeBasicPrice;

    @Value("${stripe.price.premium}")
    private String stripePremiumPrice;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${base.url}")
    private String baseUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Create a checkout session for a trial purchase (one-time)
     */
    public String createTrialCheckoutSession(String userEmail, String userId) throws StripeException {
        // Create a new Stripe customer or get existing
        Customer customer = getOrCreateCustomer(userEmail, userId);

        // Create a checkout session for the trial
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("price", stripeTrial);
        lineItem.put("quantity", 1);

        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", List.of(lineItem));
        params.put("mode", "payment"); // one-time payment
        params.put("customer", customer.getId());
        params.put("success_url", baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}&plan=TRIAL");
        params.put("cancel_url", baseUrl + "/payment/cancel");

        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("plan", "TRIAL");
        params.put("metadata", metadata);

        Session session = Session.create(params);
        return session.getUrl();
    }

    /**
     * Create a checkout session for a subscription (basic or premium)
     */
    public String createSubscriptionCheckoutSession(String userEmail, String userId, SubscriptionPlan plan) throws StripeException {
        // Create a new Stripe customer or get existing
        Customer customer = getOrCreateCustomer(userEmail, userId);

        // Get the appropriate price ID
        String priceId = plan == SubscriptionPlan.BASIC ? stripeBasicPrice : stripePremiumPrice;

        // Create a checkout session for the subscription
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("price", priceId);
        lineItem.put("quantity", 1);

        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", List.of(lineItem));
        params.put("mode", "subscription"); // subscription
        params.put("customer", customer.getId());
        params.put("success_url", baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}&plan=" + plan.name());
        params.put("cancel_url", baseUrl + "/payment/cancel");

        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("plan", plan.name());
        params.put("metadata", metadata);

        Session session = Session.create(params);
        return session.getUrl();
    }

    /**
     * Handle completed checkout session
     */
    @Transactional
    public void handleCheckoutSessionCompleted(Session session) throws StripeException {
        String userId = session.getMetadata().get("userId");
        String planName = session.getMetadata().get("plan");
        SubscriptionPlan plan = SubscriptionPlan.valueOf(planName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if user already has a subscription
        UserSubscription existingSubscription = subscriptionRepository.findById(userId).orElse(null);

        if (existingSubscription != null) {
            // Update existing subscription
            existingSubscription.setPlan(plan);
            existingSubscription.setActive(true);
            existingSubscription.setRemainingOutfits(plan.getMonthlyOutfitQuota());

            if (plan != SubscriptionPlan.TRIAL) {
                // Update with subscription details
                String subscriptionId = session.getSubscription();
                if (subscriptionId != null) {
                    Subscription subscription = Subscription.retrieve(subscriptionId);
                    existingSubscription.setStripeSubscriptionId(subscriptionId);

                    // Get next billing date
                    long nextBillingTimestamp = subscription.getCurrentPeriodEnd();
                    LocalDateTime nextBillingDate = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(nextBillingTimestamp),
                            ZoneId.systemDefault());

                    existingSubscription.setNextBillingDate(nextBillingDate);
                }
            }

            subscriptionRepository.save(existingSubscription);
        } else {
            // Create new subscription
            UserSubscription subscription = new UserSubscription(
                    userId,
                    plan,
                    plan != SubscriptionPlan.TRIAL ? session.getSubscription() : null,
                    session.getCustomer()
            );

            subscriptionRepository.save(subscription);
        }

        // Update user's premium status
        user.setPremiumUser(true);
        userRepository.save(user);
    }

    /**
     * Get or create Stripe customer
     */
    private Customer getOrCreateCustomer(String email, String userId) throws StripeException {
        // Check if user already has a subscription with customer ID
        UserSubscription subscription = subscriptionRepository.findById(userId).orElse(null);

        if (subscription != null && subscription.getStripeCustomerId() != null) {
            return Customer.retrieve(subscription.getStripeCustomerId());
        }

        // Create new customer
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("email", email);
        customerParams.put("metadata", Map.of("userId", userId));

        return Customer.create(customerParams);
    }

    /**
     * Cancel a subscription
     */
    @Transactional
    public void cancelSubscription(String userId) throws StripeException {
        UserSubscription subscription = subscriptionRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user"));

        if (subscription.getStripeSubscriptionId() != null) {
            // Cancel at period end (user keeps access until end of billing period)
            Subscription stripeSubscription = Subscription.retrieve(subscription.getStripeSubscriptionId());
            Map<String, Object> cancelParams = new HashMap<>();
            cancelParams.put("cancel_at_period_end", true);
            stripeSubscription.update(cancelParams);

            // Update local record
            subscription.setActive(false);
            subscriptionRepository.save(subscription);
        } else {
            // For trial subscriptions, just deactivate
            subscription.setActive(false);
            subscriptionRepository.save(subscription);
        }
    }

    /**
     * Handle subscription renewal/cancellation
     */
    @Transactional
    public void handleSubscriptionUpdated(Subscription stripeSubscription) {
        try {
            // Find associated user subscription
            String customerId = stripeSubscription.getCustomer();
            List<UserSubscription> userSubscriptions = subscriptionRepository.findAll().stream()
                    .filter(sub -> customerId.equals(sub.getStripeCustomerId()))
                    .toList();

            if (userSubscriptions.isEmpty()) {
                log.warn("No user subscription found for Stripe customer ID: {}", customerId);
                return;
            }

            UserSubscription subscription = userSubscriptions.get(0);

            // Update subscription status
            String status = stripeSubscription.getStatus();
            switch (status) {
                case "active":
                    // Subscription is active, reset quota if it's a renewal
                    subscription.setActive(true);
                    long currentPeriodStart = stripeSubscription.getCurrentPeriodStart();
                    LocalDateTime periodStart = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(currentPeriodStart),
                            ZoneId.systemDefault());

                    if (periodStart.isAfter(subscription.getSubscriptionStart())) {
                        // This is a renewal
                        subscription.resetMonthlyQuota();
                    }
                    break;
                case "canceled":
                case "unpaid":
                case "past_due":
                    // Subscription is no longer active
                    subscription.setActive(false);
                    break;
            }

            subscriptionRepository.save(subscription);

            // Update user premium status if needed
            User user = userRepository.findById(subscription.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // User is premium if they have any active subscription
            boolean isActive = "active".equals(status);
            if (user.isPremiumUser() != isActive) {
                user.setPremiumUser(isActive);
                userRepository.save(user);
            }

        } catch (Exception e) {
            log.error("Error handling subscription update", e);
        }
    }
    public String createCustomerPortalSession(String customerId) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(baseUrl + "/account")
                .build();

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);

        return session.getUrl();
    }
}