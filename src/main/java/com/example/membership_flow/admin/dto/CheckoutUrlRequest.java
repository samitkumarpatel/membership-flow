package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckoutUrlRequest(
        @JsonProperty("variant_id") String variantId,
        @JsonProperty("selling_plan_id") String sellingPlanId
) {}
