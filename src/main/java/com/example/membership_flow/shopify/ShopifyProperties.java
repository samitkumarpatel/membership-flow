package com.example.membership_flow.shopify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopify")
public record ShopifyProperties(
        String storeDomain,
        String clientId,
        String clientSecret,
        String apiVersion
) {}
