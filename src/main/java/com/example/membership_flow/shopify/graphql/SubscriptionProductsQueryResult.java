package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SubscriptionProductsQueryResult(Data data) {
    public record Data(SellingPlanGroupConnection sellingPlanGroups) {}
    public record SellingPlanGroupConnection(List<SellingPlanGroupEdge> edges) {}
    public record SellingPlanGroupEdge(SellingPlanGroupNode node) {}
    public record SellingPlanGroupNode(
            String id,
            String name,
            SellingPlanConnection sellingPlans,
            ProductConnection products
    ) {}
    public record SellingPlanConnection(List<SellingPlanEdge> edges) {}
    public record SellingPlanEdge(SellingPlanNode node) {}
    public record SellingPlanNode(
            String id,
            String name,
            BillingPolicy billingPolicy,
            List<PricingPolicy> pricingPolicies
    ) {}
    public record BillingPolicy(String interval, Integer intervalCount) {}
    public record PricingPolicy(String adjustmentType, AdjustmentValue adjustmentValue) {}
    public record AdjustmentValue(Double percentage) {}
    public record ProductConnection(List<ProductEdge> edges) {}
    public record ProductEdge(ProductNode node) {}
    public record ProductNode(
            String id,
            String title,
            String status,
            FeaturedImage featuredImage,
            VariantConnection variants
    ) {}
    public record FeaturedImage(String url, String altText) {}
    public record VariantConnection(List<VariantEdge> edges) {}
    public record VariantEdge(VariantNode node) {}
    public record VariantNode(String id, String price) {}
}
