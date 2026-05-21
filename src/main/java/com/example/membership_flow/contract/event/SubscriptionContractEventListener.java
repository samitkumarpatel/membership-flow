package com.example.membership_flow.contract.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Central listener for subscription lifecycle events.
 *
 * Each handler is intentionally thin — add downstream integrations here:
 *   - Email notifications  → inject EmailService and call it
 *   - QR code generation   → inject QrCodeService and call it
 *   - Audit / analytics    → inject AuditService and call it
 *
 * To avoid blocking the webhook response thread, annotate handlers with
 * @Async and add @EnableAsync to MembershipFlowApplication when ready.
 */
@Component
@Slf4j
public class SubscriptionContractEventListener {

    @EventListener
    public void onCreated(SubscriptionCreatedEvent event) {
        var contract = event.contract();
        log.info("[subscription.created] contractId={} customerId={} status={} currency={}",
                contract.getContractId(),
                contract.getCustomerId(),
                contract.getStatus(),
                contract.getCurrencyCode());

        // TODO: send welcome / confirmation email
        // TODO: generate QR code for the membership
        // TODO: trigger any onboarding workflow
    }

    @EventListener
    public void onUpdated(SubscriptionUpdatedEvent event) {
        var contract = event.contract();
        log.info("[subscription.updated] contractId={} status={} previousStatus={}",
                contract.getContractId(),
                contract.getStatus(),
                event.previousStatus());

        // TODO: send status-change email (e.g. cancellation, pause confirmation)
        // TODO: revoke / update QR code access on cancellation
    }
}
