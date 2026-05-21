package com.example.membership_flow.admin.dto;

public record BillingAttemptRequest(String contractId, int attemptNumber) {
    public BillingAttemptRequest(String contractId) {
        this(contractId, 1);
    }
}
