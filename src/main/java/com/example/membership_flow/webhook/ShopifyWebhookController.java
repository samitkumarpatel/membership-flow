package com.example.membership_flow.webhook;

import com.example.membership_flow.billing.BillingAttemptRecord;
import com.example.membership_flow.billing.BillingAttemptRepository;
import com.example.membership_flow.contract.SubscriptionContractWebhookService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/webhooks/shopify")
@Slf4j
@RequiredArgsConstructor
public class ShopifyWebhookController {

    private final BillingAttemptRepository billingAttemptRepository;
    private final SubscriptionContractWebhookService contractWebhookService;
    private final ShopifyHmacVerifier hmacVerifier;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BillingAttemptPayload(
            @JsonProperty("admin_graphql_api_id") String adminGraphqlApiId,
            @JsonProperty("error_code")           String errorCode,
            @JsonProperty("error_message")        String errorMessage
    ) {}

    /**
     * Single entry point for all Shopify webhooks.
     * Raw body is kept as String so HMAC signature verification can be added
     * here later without needing to re-read an already-consumed stream.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void handleWebhook(
            @RequestHeader("X-Shopify-Topic") String topic,
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmacHeader,
            @RequestBody String rawBody) {

        if (!hmacVerifier.verify(rawBody, hmacHeader)) {
            log.warn("Webhook rejected — HMAC verification failed: topic={}", topic);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid HMAC signature");
        }

        log.debug("Shopify webhook received: topic={}", topic);

        try {
            switch (topic) {
                case "subscription_billing_attempts/success" -> {
                    var p = objectMapper.readValue(rawBody, BillingAttemptPayload.class);
                    onBillingSuccess(p);
                }
                case "subscription_billing_attempts/failure" -> {
                    var p = objectMapper.readValue(rawBody, BillingAttemptPayload.class);
                    onBillingFailure(p);
                }
                case "subscription_contracts/create" -> {
                    var p = objectMapper.readValue(rawBody, SubscriptionContractWebhookService.ContractPayload.class);
                    contractWebhookService.onCreate(p);
                }
                case "subscription_contracts/update" -> {
                    var p = objectMapper.readValue(rawBody, SubscriptionContractWebhookService.ContractPayload.class);
                    contractWebhookService.onUpdate(p);
                }
                default -> log.debug("Ignored webhook topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Failed to process webhook topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private void onBillingSuccess(BillingAttemptPayload payload) {
        billingAttemptRepository.findByAttemptId(payload.adminGraphqlApiId()).ifPresentOrElse(
                record -> {
                    record.setStatus(BillingAttemptRecord.Status.SUCCESS);
                    record.setNextRetryAt(null);
                    record.setUpdatedAt(Instant.now());
                    billingAttemptRepository.save(record);
                    log.info("Billing attempt succeeded: {}", payload.adminGraphqlApiId());
                },
                () -> log.warn("No record found for successful attempt: {}", payload.adminGraphqlApiId())
        );
    }

    private void onBillingFailure(BillingAttemptPayload payload) {
        billingAttemptRepository.findByAttemptId(payload.adminGraphqlApiId()).ifPresentOrElse(
                record -> {
                    record.setStatus(BillingAttemptRecord.Status.FAILED);
                    record.setErrorCode(payload.errorCode());
                    record.setErrorMessage(payload.errorMessage());
                    record.setNextRetryAt(Instant.now().plus(1, ChronoUnit.DAYS));
                    record.setUpdatedAt(Instant.now());
                    billingAttemptRepository.save(record);
                    log.warn("Billing attempt failed: {} errorCode={}", payload.adminGraphqlApiId(), payload.errorCode());
                },
                () -> log.warn("No record found for failed attempt: {}", payload.adminGraphqlApiId())
        );
    }
}
