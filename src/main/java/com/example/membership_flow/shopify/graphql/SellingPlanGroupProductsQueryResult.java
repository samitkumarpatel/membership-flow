package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record SellingPlanGroupProductsQueryResult(Data data) {

    public record Data(SellingPlanGroupNode sellingPlanGroup) {}

    public record SellingPlanGroupNode(String id, String name, ProductConnection products) {}

    public record ProductConnection(List<ProductEdge> edges) {}

    public record ProductEdge(ProductNode node) {}

    public record ProductNode(String id, String title, String status, VariantConnection variants) {}

    public record VariantConnection(List<VariantEdge> edges) {}

    public record VariantEdge(VariantNode node) {}

    public record VariantNode(String id) {}
}
