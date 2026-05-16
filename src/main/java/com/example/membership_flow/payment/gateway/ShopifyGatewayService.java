package com.example.membership_flow.payment.gateway;

import com.example.membership_flow.member.Member;
import com.example.membership_flow.plan.MembershipPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Stub implementation — replace method bodies with real Shopify Subscriptions API calls
 * once shopify.access-token and shopify.shop-domain are configured.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.application.payment.shopify.enabled", havingValue = "true")
public class ShopifyGatewayService implements PaymentGateway {

    @Override
    public MembershipPlan.Gateway supports() {
        return MembershipPlan.Gateway.SHOPIFY;
    }

    @Override
    public GatewaySubscription createSubscription(Member member, MembershipPlan plan) {
        log.info("STUB: Creating Shopify subscription for member={} plan={}", member.getId(), plan.getId());
        // TODO: Use Shopify Subscriptions API (appSubscriptionCreate mutation)
        //       Return a confirmationUrl as clientSecret for redirect-based flow
        return new GatewaySubscription(
                "shopify_sub_stub_" + member.getId(),
                "shopify_cus_stub_" + member.getId(),
                "https://stub-confirmation-url",
                "PENDING",
                Instant.now().plusSeconds(60L * 60 * 24 * plan.getIntervalDays())
        );
    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        log.info("STUB: Cancelling Shopify subscription={}", gatewaySubscriptionId);
        // TODO: appSubscriptionCancel mutation
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        log.info("STUB: Parsing Shopify webhook");
        // TODO: Verify HMAC signature, parse JSON body
        return new WebhookEvent("app_subscriptions/update", "shopify_sub_stub", "ACTIVE");
    }
}
