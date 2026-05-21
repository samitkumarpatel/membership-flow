package com.example.membership_flow.admin.dto;

import com.example.membership_flow.billing.BillingAttemptInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SubscriptionContractsResponse(int total, List<ContractItem> contracts) {

    public record ContractItem(
            String id,
            String status,
            @JsonProperty("next_billing_date") String nextBillingDate,
            @JsonProperty("created_at") String createdAt,
            CustomerInfo customer,
            BillingPolicy billing,
            List<LineItem> lines,
            List<OrderRef> orders,
            List<BillingAttemptInfo> billingAttempts
    ) {}

    public record CustomerInfo(
            String id,
            String email,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName
    ) {}

    public record BillingPolicy(String interval, @JsonProperty("interval_count") int intervalCount) {}

    public record LineItem(
            String title,
            int quantity,
            @JsonProperty("selling_plan_name") String sellingPlanName,
            @JsonProperty("selling_plan_id") String sellingPlanId,
            String price,
            String currency
    ) {}

    public record OrderRef(String id, String name, @JsonProperty("created_at") String createdAt) {}
}
