package com.example.membership_flow.payment;

import com.example.membership_flow.membership.MembershipService;
import com.example.membership_flow.plan.MembershipPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final MembershipService membershipService;

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("Received Stripe webhook");
        membershipService.handleGatewayEvent(MembershipPlan.Gateway.STRIPE, payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/shopify")
    public ResponseEntity<Void> shopifyWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String signature) {
        log.info("Received Shopify webhook");
        membershipService.handleGatewayEvent(MembershipPlan.Gateway.SHOPIFY, payload, signature);
        return ResponseEntity.ok().build();
    }
}
