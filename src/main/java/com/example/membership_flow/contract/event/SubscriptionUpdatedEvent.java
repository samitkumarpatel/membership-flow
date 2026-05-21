package com.example.membership_flow.contract.event;

import com.example.membership_flow.contract.SubscriptionContractRecord;

/**
 * Fired when Shopify reports a status change on an existing contract (subscription_contracts/update).
 * Use previousStatus to gate logic (e.g. send cancellation email only when ACTIVE → CANCELLED).
 */
public record SubscriptionUpdatedEvent(SubscriptionContractRecord contract, String previousStatus) {}
