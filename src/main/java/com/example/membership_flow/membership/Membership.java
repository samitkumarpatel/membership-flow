package com.example.membership_flow.membership;

import com.example.membership_flow.plan.MembershipPlan;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "memberships")
public class Membership {

    @Id
    private String id;

    @Indexed
    private String memberId;

    private String planId;

    private MembershipStatus status;
    private MembershipPlan.Gateway gateway;

    private String gatewaySubscriptionId;
    private String gatewayCustomerId;
    private String clientSecret;        // returned to client to confirm payment

    private Instant currentPeriodEnd;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public enum MembershipStatus {
        PENDING,    // payment not yet confirmed
        ACTIVE,
        CANCELLED,
        EXPIRED,
        FAILED
    }
}
