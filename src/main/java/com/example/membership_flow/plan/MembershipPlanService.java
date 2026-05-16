package com.example.membership_flow.plan;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository repository;

    public MembershipPlan create(MembershipPlanRequest request) {
        var plan = MembershipPlan.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .currency(request.currency())
                .intervalDays(request.intervalDays())
                .benefits(request.benefits())
                .gateway(request.gateway())
                .status(MembershipPlan.PlanStatus.ACTIVE)
                .build();
        return repository.save(plan);
    }

    public List<MembershipPlan> findAll() {
        return repository.findAll();
    }

    public List<MembershipPlan> findActive() {
        return repository.findByStatus(MembershipPlan.PlanStatus.ACTIVE);
    }

    public MembershipPlan findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
    }

    public MembershipPlan update(String id, MembershipPlanRequest request) {
        var plan = findById(id);
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setCurrency(request.currency());
        plan.setIntervalDays(request.intervalDays());
        plan.setBenefits(request.benefits());
        plan.setGateway(request.gateway());
        return repository.save(plan);
    }

    public MembershipPlan setStatus(String id, MembershipPlan.PlanStatus status) {
        var plan = findById(id);
        plan.setStatus(status);
        return repository.save(plan);
    }
}
