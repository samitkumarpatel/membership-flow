package com.example.membership_flow.shopify.graphql;

import java.util.List;

public record UserError(List<String> field, String message) {}
