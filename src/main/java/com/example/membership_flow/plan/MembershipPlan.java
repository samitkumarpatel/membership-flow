package com.example.membership_flow.plan;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@Document(collection = "membership_plans")
public class MembershipPlan {

    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private int intervalDays;          // billing cycle length
    private List<String> benefits;
    private PlanStatus status;
    private Gateway gateway;           // STRIPE or SHOPIFY

    // Gateway-side plan/product IDs (populated after sync)
    private String gatewayPlanId;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public enum PlanStatus { ACTIVE, INACTIVE, ARCHIVED }
    public enum Gateway { STRIPE, SHOPIFY }
}
