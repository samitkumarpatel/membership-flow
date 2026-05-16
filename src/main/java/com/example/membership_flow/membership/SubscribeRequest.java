package com.example.membership_flow.membership;

import com.example.membership_flow.plan.MembershipPlan;

public record SubscribeRequest(String planId, MembershipPlan.Gateway gateway) {}
