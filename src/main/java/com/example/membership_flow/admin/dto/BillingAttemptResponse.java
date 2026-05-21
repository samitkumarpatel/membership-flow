package com.example.membership_flow.admin.dto;

public record BillingAttemptResponse(
        String attemptId,
        boolean ready,
        String errorCode,
        String errorMessage,
        String orderId,
        String orderName,
        String orderAmount,
        String orderCurrency
) {}
