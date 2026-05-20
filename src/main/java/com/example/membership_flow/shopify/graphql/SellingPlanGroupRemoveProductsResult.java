package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SellingPlanGroupRemoveProductsResult(Data data) {

    public record Data(Payload sellingPlanGroupRemoveProducts) {}

    public record Payload(
            SellingPlanGroup sellingPlanGroup,
            List<UserError> userErrors
    ) {}

    public record SellingPlanGroup(String id) {}
}
