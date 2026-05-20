package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SubscriptionContractsQueryResult(Data data) {
    public record Data(SubscriptionContractConnection subscriptionContracts) {}
    public record SubscriptionContractConnection(List<SubscriptionContractEdge> edges) {}
    public record SubscriptionContractEdge(SubscriptionContractNode node) {}
    public record SubscriptionContractNode(
            String id,
            String status,
            String nextBillingDate,
            String createdAt,
            Customer customer,
            SubscriptionLineConnection lines,
            SubscriptionBillingPolicy billingPolicy,
            OrderConnection orders
    ) {}
    public record Customer(String id, String email, String firstName, String lastName) {}
    public record SubscriptionLineConnection(List<SubscriptionLineEdge> edges) {}
    public record SubscriptionLineEdge(SubscriptionLineNode node) {}
    public record SubscriptionLineNode(
            String id,
            String title,
            int quantity,
            String sellingPlanName,
            String sellingPlanId,
            MoneyV2 currentPrice
    ) {}
    public record MoneyV2(String amount, String currencyCode) {}
    public record SubscriptionBillingPolicy(String interval, int intervalCount) {}
    public record OrderConnection(List<OrderEdge> edges) {}
    public record OrderEdge(OrderNode node) {}
    public record OrderNode(String id, String name, String createdAt) {}
}
