package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LinkProductRequest(
        @JsonProperty("selling_plan_group_id") String sellingPlanGroupId,
        @JsonProperty("product_ids") List<String> productIds
) {}
