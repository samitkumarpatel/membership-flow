package com.example.membership_flow.contract;

import com.example.membership_flow.contract.event.SubscriptionCreatedEvent;
import com.example.membership_flow.contract.event.SubscriptionUpdatedEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionContractWebhookService {

    private final SubscriptionContractRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Inbound payload (Shopify REST webhook body) ───────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContractPayload(
            @JsonProperty("admin_graphql_api_id")           String adminGraphqlApiId,
            @JsonProperty("customer_admin_graphql_api_id")  String customerAdminGraphqlApiId,
            String status,
            @JsonProperty("currency_code")                  String currencyCode,
            @JsonProperty("next_billing_date")              String nextBillingDate,
            @JsonProperty("created_at")                     String createdAt,
            @JsonProperty("billing_policy")                 BillingPolicyPayload billingPolicy,
            List<LinePayload> lines
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BillingPolicyPayload(
            String interval,
            @JsonProperty("interval_count") int intervalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LinePayload(
            String title,
            Integer quantity,
            @JsonProperty("current_price") String currentPrice,
            @JsonProperty("selling_plan_id")   String sellingPlanId,
            @JsonProperty("selling_plan_name") String sellingPlanName,
            @JsonProperty("product_id")  String productId,
            @JsonProperty("variant_id")  String variantId
    ) {}

    // ── Handlers ─────────────────────────────────────────────────────────────

    public void onCreate(ContractPayload payload) {
        var record = repository.findByContractId(payload.adminGraphqlApiId())
                .orElseGet(() -> SubscriptionContractRecord.builder()
                        .contractId(payload.adminGraphqlApiId())
                        .createdAt(Instant.now())
                        .build());

        applyPayload(record, payload);
        repository.save(record);

        log.info("Subscription contract created: contractId={} customerId={} status={}",
                record.getContractId(), record.getCustomerId(), record.getStatus());

        eventPublisher.publishEvent(new SubscriptionCreatedEvent(record));
    }

    public void onUpdate(ContractPayload payload) {
        var existing = repository.findByContractId(payload.adminGraphqlApiId());
        var previousStatus = existing.map(SubscriptionContractRecord::getStatus).orElse(null);

        var record = existing.orElseGet(() -> SubscriptionContractRecord.builder()
                .contractId(payload.adminGraphqlApiId())
                .createdAt(Instant.now())
                .build());

        applyPayload(record, payload);
        repository.save(record);

        log.info("Subscription contract updated: contractId={} status={} previousStatus={}",
                record.getContractId(), record.getStatus(), previousStatus);

        eventPublisher.publishEvent(new SubscriptionUpdatedEvent(record, previousStatus));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private void applyPayload(SubscriptionContractRecord record, ContractPayload payload) {
        record.setCustomerId(payload.customerAdminGraphqlApiId());
        record.setStatus(payload.status());
        record.setCurrencyCode(payload.currencyCode());
        record.setNextBillingDate(payload.nextBillingDate());
        record.setUpdatedAt(Instant.now());

        if (payload.billingPolicy() != null) {
            record.setBillingPolicy(new SubscriptionContractRecord.BillingPolicy(
                    payload.billingPolicy().interval(),
                    payload.billingPolicy().intervalCount()));
        }

        if (payload.lines() != null) {
            record.setLines(payload.lines().stream()
                    .map(l -> SubscriptionContractRecord.ContractLine.builder()
                            .title(l.title())
                            .quantity(l.quantity() != null ? l.quantity() : 1)
                            .price(l.currentPrice())
                            .sellingPlanId(l.sellingPlanId())
                            .sellingPlanName(l.sellingPlanName())
                            .productId(l.productId())
                            .variantId(l.variantId())
                            .build())
                    .toList());
        }

        if (record.getShopifyCreatedAt() == null && payload.createdAt() != null) {
            try {
                record.setShopifyCreatedAt(Instant.parse(payload.createdAt()));
            } catch (Exception ignored) {}
        }
    }
}
