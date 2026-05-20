package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SellingPlanGroupUpdateResult(Data data) {
    public record Data(Payload sellingPlanGroupUpdate) {}
    public record Payload(SellingPlanGroup sellingPlanGroup, List<UserError> userErrors) {}
    public record SellingPlanGroup(String id) {}
}
