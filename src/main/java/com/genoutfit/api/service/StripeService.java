package com.genoutfit.api.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PriceRetrieveParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StripeService {
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.product.price.id}")
    private String stripeProductPriceId;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${base.url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createCheckoutSession(String userEmail, String submissionId) {
        try {
            // Create a new Stripe customer
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", userEmail);
            Customer customer = Customer.create(customerParams);

            // Create a Checkout Session
            List<Object> paymentMethodTypes = new ArrayList<>();
            paymentMethodTypes.add("card");

            Map<String, Object> lineItem = new HashMap<>();
            lineItem.put("price", stripeProductPriceId);
            lineItem.put("quantity", 1);

            Map<String, Object> params = new HashMap<>();
            params.put("payment_method_types", paymentMethodTypes);
            params.put("line_items", Collections.singletonList(lineItem));
            params.put("mode", "payment");
            params.put("customer", customer.getId());
            params.put("success_url", baseUrl + "/api/payment/success?session_id={CHECKOUT_SESSION_ID}");
            params.put("cancel_url", baseUrl);

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("submissionId", submissionId);
            params.put("metadata", metadata);

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            e.printStackTrace();
            return null;
        }
    }

    public long getPrice() throws StripeException {
        PriceRetrieveParams params = PriceRetrieveParams.builder()
                .addExpand("product")
                .build();

        return Price.retrieve(stripeProductPriceId, params, null).getUnitAmountDecimal().longValue();
    }

    public Event constructEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(
                    payload,
                    sigHeader,
                    stripeWebhookSecret
            );
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid signature", e);
        }
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}