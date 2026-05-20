package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SellingPlanGroupUpdateResult(Data data) {
    public record Data(Payload sellingPlanGroupUpdate) {}
    public record Payload(SellingPlanGroup sellingPlanGroup, List<UserError> userErrors) {}
    public record SellingPlanGroup(String id, SellingPlanConnection sellingPlans) {}
    public record SellingPlanConnection(List<SellingPlanEdge> edges) {}
    public record SellingPlanEdge(SellingPlanNode node) {}
    public record SellingPlanNode(String id) {}
}
