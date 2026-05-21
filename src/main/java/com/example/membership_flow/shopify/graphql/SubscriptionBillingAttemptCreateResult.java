package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SubscriptionBillingAttemptCreateResult(Data data) {
    public record Data(Payload subscriptionBillingAttemptCreate) {}
    public record Payload(BillingAttempt subscriptionBillingAttempt, List<UserError> userErrors) {}
    public record BillingAttempt(String id, boolean ready, String errorCode, String errorMessage, Order order) {}
    public record Order(String id, String name, TotalPriceSet totalPriceSet) {}
    public record TotalPriceSet(ShopMoney shopMoney) {}
    public record ShopMoney(String amount, String currencyCode) {}
}
