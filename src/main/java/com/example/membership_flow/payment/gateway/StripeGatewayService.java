package com.example.membership_flow.payment.gateway;

import com.example.membership_flow.member.Member;
import com.example.membership_flow.plan.MembershipPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Stub implementation — replace method bodies with real Stripe SDK calls once
 * stripe-java is added to pom.xml and stripe.secret-key is configured.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.application.payment.stripe.enabled", havingValue = "true", matchIfMissing = true)
public class StripeGatewayService implements PaymentGateway {

    @Override
    public MembershipPlan.Gateway supports() {
        return MembershipPlan.Gateway.STRIPE;
    }

    @Override
    public GatewaySubscription createSubscription(Member member, MembershipPlan plan) {
        log.info("STUB: Creating Stripe subscription for member={} plan={}", member.getId(), plan.getId());
        // TODO: 1. Create or retrieve Stripe Customer using member.email
        //       2. Create Stripe Price from plan (if not already synced via plan.gatewayPlanId)
        //       3. Create Stripe Subscription → returns client_secret for frontend
        return new GatewaySubscription(
                "sub_stub_" + member.getId(),
                "cus_stub_" + member.getId(),
                "pi_stub_secret",
                "incomplete",
                Instant.now().plusSeconds(60L * 60 * 24 * plan.getIntervalDays())
        );
    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        log.info("STUB: Cancelling Stripe subscription={}", gatewaySubscriptionId);
        // TODO: Stripe.subscriptions.cancel(gatewaySubscriptionId)
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        log.info("STUB: Parsing Stripe webhook");
        // TODO: Stripe.Webhook.constructEvent(payload, signature, endpointSecret)
        //       Map event.type → WebhookEvent
        return new WebhookEvent("customer.subscription.updated", "sub_stub", "active");
    }
}
