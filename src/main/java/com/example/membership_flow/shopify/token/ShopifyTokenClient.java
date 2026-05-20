package com.example.membership_flow.shopify.token;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface ShopifyTokenClient {

    @PostExchange("/admin/oauth/access_token")
    ShopifyTokenResponse exchange(@RequestBody ShopifyTokenRequest request);
}
