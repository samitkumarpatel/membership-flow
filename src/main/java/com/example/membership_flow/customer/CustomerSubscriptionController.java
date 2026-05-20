package com.example.membership_flow.customer;

import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.customer.dto.CustomerLookupRequest;
import com.example.membership_flow.customer.dto.CustomerSubscriptionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerSubscriptionController {

    private final CustomerSubscriptionService customerSubscriptionService;

    @PostMapping("/subscriptions/lookup")
    public CustomerSubscriptionsResponse lookup(@RequestBody CustomerLookupRequest request) {
        return customerSubscriptionService.lookup(request);
    }

    @PostMapping("/subscriptions/pause")
    public LinkProductResponse pause(@RequestParam String contractId) {
        return customerSubscriptionService.pause(contractId);
    }

    @PostMapping("/subscriptions/resume")
    public LinkProductResponse resume(@RequestParam String contractId) {
        return customerSubscriptionService.resume(contractId);
    }

    @PostMapping("/subscriptions/cancel")
    public LinkProductResponse cancel(@RequestParam String contractId) {
        return customerSubscriptionService.cancel(contractId);
    }
}
