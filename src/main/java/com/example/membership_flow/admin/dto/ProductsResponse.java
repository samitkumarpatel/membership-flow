package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProductsResponse(int total, List<ProductItem> products) {
    public record ProductItem(
            String id,
            String title,
            String status,
            @JsonProperty("variant_id") String variantId
    ) {}
}
