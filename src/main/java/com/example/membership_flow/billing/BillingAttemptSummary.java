package com.example.membership_flow.billing;

import java.time.Instant;
import java.time.LocalDate;

public record BillingAttemptSummary(
        String attemptId,
        String status,
        int attemptCount,
        LocalDate scheduledDate,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant nextRetryAt
) {
    public static BillingAttemptSummary from(BillingAttemptRecord r) {
        return new BillingAttemptSummary(
                r.getAttemptId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getAttemptCount(),
                r.getScheduledDate(),
                r.getErrorCode(),
                r.getErrorMessage(),
                r.getCreatedAt(),
                r.getNextRetryAt()
        );
    }
}
