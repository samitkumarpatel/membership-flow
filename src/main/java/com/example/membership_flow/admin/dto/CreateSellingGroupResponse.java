package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateSellingGroupResponse(
        String status,
        @JsonProperty("selling_plan_group_id") String sellingPlanGroupId,
        @JsonProperty("selling_plan_ids") List<String> sellingPlanIds
) {}
