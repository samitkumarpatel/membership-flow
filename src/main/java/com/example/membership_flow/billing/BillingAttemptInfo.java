package com.example.membership_flow.billing;

public record BillingAttemptInfo(
        String id,
        boolean ready,
        String errorCode,
        String errorMessage,
        String createdAt,
        String orderId,
        String orderName
) {}
