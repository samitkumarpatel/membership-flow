package com.example.membership_flow.admin.dto;

import java.util.List;

public record SellingGroupsResponse(List<SellingGroupItem> sellingPlanGroups) {

    public record SellingGroupItem(
            String id,
            String name,
            String merchantCode,
            int productsCount,
            List<SellingPlanItem> sellingPlans
    ) {}

    public record SellingPlanItem(
            String id,
            String name,
            String interval,
            int intervalCount,
            Double discountPercentage
    ) {}
}
