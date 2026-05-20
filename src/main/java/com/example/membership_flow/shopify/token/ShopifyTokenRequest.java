package com.example.membership_flow.shopify.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShopifyTokenRequest(
        @JsonProperty("client_id")     String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("grant_type")    String grantType,
        @JsonProperty("refresh_token") String refreshToken
) {
    static ShopifyTokenRequest clientCredentials(String clientId, String clientSecret) {
        return new ShopifyTokenRequest(clientId, clientSecret, "client_credentials", null);
    }

    static ShopifyTokenRequest refresh(String clientId, String clientSecret, String refreshToken) {
        return new ShopifyTokenRequest(clientId, clientSecret, "refresh_token", refreshToken);
    }
}
