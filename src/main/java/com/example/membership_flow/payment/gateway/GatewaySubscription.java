package com.example.membership_flow.payment.gateway;

import java.time.Instant;

public record GatewaySubscription(
        String gatewaySubscriptionId,
        String gatewayCustomerId,
        String clientSecret,      // Stripe: payment_intent client_secret for frontend confirmation
        String status,
        Instant currentPeriodEnd
) {}
