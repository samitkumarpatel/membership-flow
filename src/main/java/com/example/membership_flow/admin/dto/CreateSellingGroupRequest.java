package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateSellingGroupRequest(
        String name,
        @JsonProperty("selling_plans") List<SellingPlanInput> sellingPlans
) {
    public record SellingPlanInput(
            String interval,
            @JsonProperty("interval_count") int intervalCount,
            @JsonProperty("discount_percentage") double discountPercentage
    ) {}
}
