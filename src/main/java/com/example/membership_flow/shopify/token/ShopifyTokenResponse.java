package com.example.membership_flow.shopify.token;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Non-expiring token: access_token + scope only.
 * Expiring token: adds expires_in, refresh_token, refresh_token_expires_in.
 */
public record ShopifyTokenResponse(
        @JsonProperty("access_token")             String accessToken,
        @JsonProperty("scope")                    String scope,
        @JsonProperty("expires_in")               Long expiresIn,
        @JsonProperty("refresh_token")            String refreshToken,
        @JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn
) {
    public boolean isExpiring() {
        return expiresIn != null;
    }
}
