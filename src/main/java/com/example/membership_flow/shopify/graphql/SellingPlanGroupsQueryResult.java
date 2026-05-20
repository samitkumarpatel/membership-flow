package com.example.membership_flow.shopify.graphql;

import java.util.List;

/**
 * Maps the Shopify GraphQL response for the sellingPlanGroups query.
 * Inline-fragment fields (billingPolicy, pricingPolicies) are merged directly
 * into the JSON object by Shopify, so plain records work without discriminators.
 */
public record SellingPlanGroupsQueryResult(Data data) {

    public record Data(SellingPlanGroupConnection sellingPlanGroups) {}

    public record SellingPlanGroupConnection(List<SellingPlanGroupEdge> edges) {}

    public record SellingPlanGroupEdge(SellingPlanGroupNode node) {}

    public record SellingPlanGroupNode(
            String id,
            String name,
            String merchantCode,
            SellingPlanConnection sellingPlans,
            ProductsCount productsCount
    ) {}

    public record SellingPlanConnection(List<SellingPlanEdge> edges) {}

    public record SellingPlanEdge(SellingPlanNode node) {}

    public record SellingPlanNode(
            String id,
            String name,
            BillingPolicy billingPolicy,
            List<PricingPolicy> pricingPolicies
    ) {}

    /** Fields from ... on SellingPlanRecurringBillingPolicy */
    public record BillingPolicy(String interval, Integer intervalCount) {}

    /** Fields from ... on SellingPlanFixedPricingPolicy */
    public record PricingPolicy(String adjustmentType, AdjustmentValue adjustmentValue) {}

    /** Fields from ... on SellingPlanPricingPolicyPercentageValue */
    public record AdjustmentValue(Double percentage) {}

    public record ProductsCount(Integer count) {}
}
