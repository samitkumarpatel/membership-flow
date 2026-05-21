package com.example.membership_flow.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "subscription_contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionContractRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String contractId;         // gid://shopify/SubscriptionContract/...

    private String customerId;         // gid://shopify/Customer/...
    private String status;             // ACTIVE, PAUSED, CANCELLED, EXPIRED, FAILED
    private String currencyCode;
    private String nextBillingDate;

    private BillingPolicy billingPolicy;
    private List<ContractLine> lines;

    private Instant shopifyCreatedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingPolicy {
        private String interval;
        private int intervalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractLine {
        private String title;
        private int quantity;
        private String price;
        private String sellingPlanId;
        private String sellingPlanName;
        private String productId;
        private String variantId;
    }
}
