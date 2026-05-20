package com.example.membership_flow.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SubscriptionProductsResponse(int total, List<ProductEntry> products) {

    public record ProductEntry(
            String id,
            String title,
            String status,
            @JsonProperty("variant_id") String variantId,
            String price,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("image_alt") String imageAlt,
            List<GroupEntry> groups
    ) {}

    public record GroupEntry(
            @JsonProperty("group_id") String groupId,
            @JsonProperty("group_name") String groupName,
            List<PlanEntry> plans
    ) {}

    public record PlanEntry(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("plan_name") String planName,
            String interval,
            @JsonProperty("interval_count") int intervalCount,
            @JsonProperty("discount_percentage") Double discountPercentage,
            @JsonProperty("checkout_url") String checkoutUrl
    ) {}
}
