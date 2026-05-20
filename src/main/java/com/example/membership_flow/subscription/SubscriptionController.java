package com.example.membership_flow.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/products")
    public SubscriptionProductsResponse listSubscriptionProducts() {
        return subscriptionService.listSubscriptionProducts();
    }
}
