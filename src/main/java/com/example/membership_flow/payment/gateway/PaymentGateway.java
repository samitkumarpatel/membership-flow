package com.example.membership_flow.payment.gateway;

import com.example.membership_flow.member.Member;
import com.example.membership_flow.plan.MembershipPlan;

public interface PaymentGateway {

    MembershipPlan.Gateway supports();

    GatewaySubscription createSubscription(Member member, MembershipPlan plan);

    void cancelSubscription(String gatewaySubscriptionId);

    WebhookEvent parseWebhook(String payload, String signature);

    record WebhookEvent(String type, String subscriptionId, String status) {}
}
