package com.example.membership_flow.plan;

import java.math.BigDecimal;
import java.util.List;

public record MembershipPlanRequest(
        String name,
        String description,
        BigDecimal price,
        String currency,
        int intervalDays,
        List<String> benefits,
        MembershipPlan.Gateway gateway
) {}
