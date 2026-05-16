package com.example.membership_flow.membership;

import com.example.membership_flow.member.MemberService;
import com.example.membership_flow.payment.gateway.PaymentGateway;
import com.example.membership_flow.plan.MembershipPlan;
import com.example.membership_flow.plan.MembershipPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository repository;
    private final MemberService memberService;
    private final MembershipPlanService planService;
    private final List<PaymentGateway> gateways;

    public Membership subscribe(String memberId, SubscribeRequest request) {
        var member = memberService.findById(memberId);
        var plan = planService.findById(request.planId());

        if (plan.getStatus() != MembershipPlan.PlanStatus.ACTIVE) {
            throw new IllegalStateException("Plan is not active: " + plan.getId());
        }

        var gateway = gatewayFor(request.gateway());
        var sub = gateway.createSubscription(member, plan);

        var membership = Membership.builder()
                .memberId(memberId)
                .planId(plan.getId())
                .status(Membership.MembershipStatus.PENDING)
                .gateway(request.gateway())
                .gatewaySubscriptionId(sub.gatewaySubscriptionId())
                .gatewayCustomerId(sub.gatewayCustomerId())
                .clientSecret(sub.clientSecret())
                .currentPeriodEnd(sub.currentPeriodEnd())
                .build();

        return repository.save(membership);
    }

    public List<Membership> findByMember(String memberId) {
        return repository.findByMemberId(memberId);
    }

    public List<Membership> findAll() {
        return repository.findAll();
    }

    public List<Membership> findByStatus(Membership.MembershipStatus status) {
        return repository.findByStatus(status);
    }

    public Membership findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + id));
    }

    public Membership cancel(String id) {
        var membership = findById(id);
        if (membership.getStatus() != Membership.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE memberships can be cancelled");
        }
        gatewayFor(membership.getGateway()).cancelSubscription(membership.getGatewaySubscriptionId());
        membership.setStatus(Membership.MembershipStatus.CANCELLED);
        return repository.save(membership);
    }

    // Called by webhook handler
    public void handleGatewayEvent(MembershipPlan.Gateway gatewayType, String payload, String signature) {
        var event = gatewayFor(gatewayType).parseWebhook(payload, signature);
        repository.findByGatewaySubscriptionId(event.subscriptionId()).ifPresent(membership -> {
            membership.setStatus(mapStatus(event.status()));
            repository.save(membership);
        });
    }

    private PaymentGateway gatewayFor(MembershipPlan.Gateway type) {
        return gateways.stream()
                .filter(g -> g.supports() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No gateway configured for: " + type));
    }

    private Membership.MembershipStatus mapStatus(String gatewayStatus) {
        return switch (gatewayStatus.toLowerCase()) {
            case "active" -> Membership.MembershipStatus.ACTIVE;
            case "cancelled", "canceled" -> Membership.MembershipStatus.CANCELLED;
            case "incomplete_expired", "expired" -> Membership.MembershipStatus.EXPIRED;
            default -> Membership.MembershipStatus.PENDING;
        };
    }
}
