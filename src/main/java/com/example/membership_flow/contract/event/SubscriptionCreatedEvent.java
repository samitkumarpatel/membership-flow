package com.example.membership_flow.contract.event;

import com.example.membership_flow.contract.SubscriptionContractRecord;

/**
 * Fired when Shopify reports a brand-new subscription contract (subscription_contracts/create).
 * Downstream handlers (email, QR code, etc.) listen to this event.
 */
public record SubscriptionCreatedEvent(SubscriptionContractRecord contract) {}
