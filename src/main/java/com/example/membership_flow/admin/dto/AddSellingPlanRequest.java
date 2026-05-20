package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AddSellingPlanRequest(
        @JsonProperty("selling_plan_group_id") String sellingPlanGroupId,
        @JsonProperty("selling_plans") List<CreateSellingGroupRequest.SellingPlanInput> sellingPlans
) {}
