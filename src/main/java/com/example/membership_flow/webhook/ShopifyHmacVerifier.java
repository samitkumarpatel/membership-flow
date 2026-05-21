package com.example.membership_flow.webhook;

import com.example.membership_flow.shopify.ShopifyProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Verifies Shopify webhook authenticity using HMAC-SHA256.
 *
 * Shopify signs each webhook request with:
 *   Base64( HMAC-SHA256( rawBody, clientSecret ) )
 * and sends it in the X-Shopify-Hmac-Sha256 header.
 *
 * MessageDigest.isEqual() is used for the final comparison to prevent
 * timing-based side-channel attacks.
 */
@Component
public class ShopifyHmacVerifier {

    private final byte[] secretBytes;

    ShopifyHmacVerifier(ShopifyProperties props) {
        this.secretBytes = props.clientSecret().getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String rawBody, String shopifyHmacHeader) {
        if (shopifyHmacHeader == null || shopifyHmacHeader.isBlank()) {
            return false;
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] expected = Base64.getDecoder().decode(shopifyHmacHeader);
            return MessageDigest.isEqual(computed, expected);
        } catch (Exception e) {
            return false;
        }
    }
}
