package com.example.membership_flow.shopify.graphql;

public record SubscriptionBillingAttemptQueryResult(Data data) {
    public record Data(BillingAttempt subscriptionBillingAttempt) {}
    public record BillingAttempt(String id, boolean ready, String errorCode, String errorMessage, Order order) {}
    public record Order(String id, String name) {}
}
