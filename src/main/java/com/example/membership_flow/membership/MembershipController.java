package com.example.membership_flow.membership;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService service;

    // ── Member-facing ─────────────────────────────────────────────────────────

    @PostMapping("/api/members/{memberId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public Membership subscribe(@PathVariable String memberId, @RequestBody SubscribeRequest request) {
        return service.subscribe(memberId, request);
    }

    @GetMapping("/api/members/{memberId}/memberships")
    public List<Membership> getMemberMemberships(@PathVariable String memberId) {
        return service.findByMember(memberId);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/memberships")
    public List<Membership> listAll(@RequestParam(required = false) Membership.MembershipStatus status) {
        return status != null ? service.findByStatus(status) : service.findAll();
    }

    @GetMapping("/api/admin/memberships/{id}")
    public Membership getOne(@PathVariable String id) {
        return service.findById(id);
    }

    @PatchMapping("/api/admin/memberships/{id}/cancel")
    public Membership cancel(@PathVariable String id) {
        return service.cancel(id);
    }
}
