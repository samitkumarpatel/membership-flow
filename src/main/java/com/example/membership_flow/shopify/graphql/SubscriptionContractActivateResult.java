package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SubscriptionContractActivateResult(Data data) {

    public record Data(Payload subscriptionContractActivate) {}

    public record Payload(
            Contract contract,
            List<UserError> userErrors
    ) {}

    public record Contract(String id, String status) {}
}
