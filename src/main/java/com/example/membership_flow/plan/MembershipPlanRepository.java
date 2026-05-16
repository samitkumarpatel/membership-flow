package com.example.membership_flow.plan;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MembershipPlanRepository extends MongoRepository<MembershipPlan, String> {
    List<MembershipPlan> findByStatus(MembershipPlan.PlanStatus status);
}
