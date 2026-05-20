package com.example.membership_flow.shopify.graphql;

import java.util.List;

/**
 * Maps the Shopify GraphQL response for sellingPlanGroupAddProducts:
 * { "data": { "sellingPlanGroupAddProducts": { "sellingPlanGroup": {...}, "userErrors": [...] } } }
 */
public record SellingPlanGroupAddProductsResult(Data data) {

    public record Data(Payload sellingPlanGroupAddProducts) {}

    public record Payload(
            SellingPlanGroup sellingPlanGroup,
            List<UserError> userErrors
    ) {}

    public record SellingPlanGroup(String id) {}
}
