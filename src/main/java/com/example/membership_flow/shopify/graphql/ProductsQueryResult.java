package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record ProductsQueryResult(Data data) {
    public record Data(ProductConnection products) {}
    public record ProductConnection(PageInfo pageInfo, List<ProductEdge> edges) {}
    public record PageInfo(boolean hasNextPage, String endCursor) {}
    public record ProductEdge(ProductNode node) {}
    public record ProductNode(String id, String title, String status, VariantConnection variants) {}
    public record VariantConnection(List<VariantEdge> edges) {}
    public record VariantEdge(VariantNode node) {}
    public record VariantNode(String id) {}
}
