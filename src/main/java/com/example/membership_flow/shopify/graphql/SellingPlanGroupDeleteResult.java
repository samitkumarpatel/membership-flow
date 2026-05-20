package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SellingPlanGroupDeleteResult(Data data) {

    public record Data(Payload sellingPlanGroupDelete) {}

    public record Payload(
            String deletedSellingPlanGroupId,
            List<UserError> userErrors
    ) {}
}
