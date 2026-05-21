package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record CustomerSubscriptionContractsQueryResult(Data data) {

    public record Data(CustomerConnection customers) {}

    public record CustomerConnection(List<CustomerEdge> edges) {}

    public record CustomerEdge(CustomerNode node) {}

    public record CustomerNode(
            String id,
            String email,
            String phone,
            String firstName,
            String lastName,
            ContractConnection subscriptionContracts
    ) {}

    public record ContractConnection(List<ContractEdge> edges) {}

    public record ContractEdge(ContractNode node) {}

    public record ContractNode(
            String id,
            String status,
            String nextBillingDate,
            String createdAt,
            BillingPolicy billingPolicy,
            LineConnection lines,
            BillingAttemptConnection billingAttempts
    ) {}

    public record BillingAttemptConnection(List<BillingAttemptEdge> edges) {}
    public record BillingAttemptEdge(BillingAttemptNode node) {}
    public record BillingAttemptNode(
            String id,
            boolean ready,
            String errorCode,
            String errorMessage,
            String createdAt,
            OrderRef order
    ) {}
    public record OrderRef(String id, String name) {}

    public record BillingPolicy(String interval, Integer intervalCount) {}

    public record LineConnection(List<LineEdge> edges) {}

    public record LineEdge(LineNode node) {}

    public record LineNode(
            String id,
            String title,
            Integer quantity,
            String sellingPlanName,
            String sellingPlanId,
            Money currentPrice
    ) {}

    public record Money(String amount, String currencyCode) {}
}
