package com.example.membership_flow.plan;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final MembershipPlanService service;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/api/plans")
    public List<MembershipPlan> listActive() {
        return service.findActive();
    }

    @GetMapping("/api/plans/{id}")
    public MembershipPlan getOne(@PathVariable String id) {
        return service.findById(id);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/plans")
    public List<MembershipPlan> listAll() {
        return service.findAll();
    }

    @PostMapping("/api/admin/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPlan create(@RequestBody MembershipPlanRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/admin/plans/{id}")
    public MembershipPlan update(@PathVariable String id, @RequestBody MembershipPlanRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/api/admin/plans/{id}/status")
    public MembershipPlan setStatus(@PathVariable String id, @RequestParam MembershipPlan.PlanStatus status) {
        return service.setStatus(id, status);
    }
}
