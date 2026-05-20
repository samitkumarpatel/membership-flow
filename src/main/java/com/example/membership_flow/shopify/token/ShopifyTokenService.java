package com.example.membership_flow.shopify.token;

import com.example.membership_flow.shopify.ShopifyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ShopifyTokenService {

    /**
     * Fall-back TTL for non-expiring tokens: re-fetch once a day as a safety net.
     */
    private static final long NON_EXPIRING_TTL_SECONDS = 86_400;

    /**
     * Renew this many seconds before the token actually expires to avoid race windows.
     */
    private static final long EXPIRY_BUFFER_SECONDS = 30;

    private record CachedToken(
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
        }
    }

    private final ShopifyTokenClient tokenClient;
    private final ShopifyProperties properties;

    private volatile CachedToken cached;

    public String getAccessToken() {
        var current = cached;
        if (current != null && !current.isExpired()) {
            return current.accessToken();
        }
        return refreshOrFetch();
    }

    private synchronized String refreshOrFetch() {
        // Re-check under lock — another thread may have refreshed already.
        var current = cached;
        if (current != null && !current.isExpired()) {
            return current.accessToken();
        }

        ShopifyTokenRequest request = (current != null && current.refreshToken() != null)
                ? ShopifyTokenRequest.refresh(properties.clientId(), properties.clientSecret(), current.refreshToken())
                : ShopifyTokenRequest.clientCredentials(properties.clientId(), properties.clientSecret());

        var response = tokenClient.exchange(request);
        cached = toCachedToken(response);
        return cached.accessToken();
    }

    private static CachedToken toCachedToken(ShopifyTokenResponse response) {
        long ttl = response.isExpiring() ? response.expiresIn() : NON_EXPIRING_TTL_SECONDS;
        return new CachedToken(
                response.accessToken(),
                response.refreshToken(),
                Instant.now().plusSeconds(ttl)
        );
    }
}
