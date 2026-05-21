package com.example.membership_flow.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "billing_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAttemptRecord {

    @Id
    private String id;

    private String contractId;

    @Indexed
    private String attemptId;       // Shopify GID — populated after the API call succeeds

    private Status status;
    private int attemptCount;
    private LocalDate scheduledDate;   // the billing cycle date this attempt belongs to
    private Instant nextRetryAt;       // set on FAILED, cleared on SUCCESS
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public enum Status { PENDING, SUCCESS, FAILED }
}
