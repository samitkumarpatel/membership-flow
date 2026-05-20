package com.example.membership_flow.customer.dto;

import java.util.List;

public record CustomerSubscriptionsResponse(
        String customerId,
        String email,
        String firstName,
        String lastName,
        int count,
        List<ContractItem> contracts
) {
    public record ContractItem(
            String id,
            String status,
            String nextBillingDate,
            String createdAt,
            BillingPolicy billing,
            List<LineItem> lines
    ) {}

    public record BillingPolicy(String interval, int intervalCount) {}

    public record LineItem(
            String title,
            int quantity,
            String sellingPlanName,
            String sellingPlanId,
            String price,
            String currency
    ) {}
}
