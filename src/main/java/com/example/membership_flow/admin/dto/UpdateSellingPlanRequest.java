package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSellingPlanRequest(
        @JsonProperty("selling_plan_group_id") String sellingPlanGroupId,
        @JsonProperty("selling_plan_id") String sellingPlanId,
        String interval,
        @JsonProperty("interval_count") int intervalCount,
        @JsonProperty("discount_percentage") double discountPercentage
) {}
