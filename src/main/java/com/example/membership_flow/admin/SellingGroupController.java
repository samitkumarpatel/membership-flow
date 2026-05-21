package com.example.membership_flow.admin;

import com.example.membership_flow.admin.dto.AddSellingPlanRequest;
import com.example.membership_flow.admin.dto.CreateSellingGroupRequest;
import com.example.membership_flow.admin.dto.CreateSellingGroupResponse;
import com.example.membership_flow.admin.dto.GroupProductsResponse;
import com.example.membership_flow.admin.dto.LinkProductRequest;
import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.admin.dto.RemoveProductRequest;
import com.example.membership_flow.admin.dto.RemoveSellingPlanRequest;
import com.example.membership_flow.admin.dto.SellingGroupsResponse;
import com.example.membership_flow.admin.dto.UpdateSellingGroupRequest;
import com.example.membership_flow.admin.dto.UpdateSellingPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/selling-groups")
@RequiredArgsConstructor
public class SellingGroupController {

    private final AdminService adminService;

    // ── Group CRUD ────────────────────────────────────────────────────────────

    @GetMapping
    public SellingGroupsResponse list() {
        return adminService.listSellingGroups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSellingGroupResponse create(@RequestBody CreateSellingGroupRequest request) {
        return adminService.createSellingGroup(request);
    }

    @PatchMapping
    public LinkProductResponse update(@RequestBody UpdateSellingGroupRequest request) {
        return adminService.updateSellingGroup(request);
    }

    @DeleteMapping
    public LinkProductResponse delete(@RequestParam String groupId) {
        return adminService.deleteSellingGroup(groupId);
    }

    // ── Products within a group ───────────────────────────────────────────────

    @GetMapping("/products")
    public GroupProductsResponse getProducts(@RequestParam String groupId) {
        return adminService.getGroupProducts(groupId);
    }

    @PostMapping("/products")
    public LinkProductResponse linkProducts(@RequestBody LinkProductRequest request) {
        return adminService.linkProductToPlan(request);
    }

    @DeleteMapping("/products")
    public LinkProductResponse removeProducts(@RequestBody RemoveProductRequest request) {
        return adminService.removeProductsFromGroup(request);
    }

    // ── Selling plans within a group ──────────────────────────────────────────

    @PostMapping("/plans")
    public LinkProductResponse addPlan(@RequestBody AddSellingPlanRequest request) {
        return adminService.addSellingPlanToGroup(request);
    }

    @PatchMapping("/plans")
    public LinkProductResponse updatePlan(@RequestBody UpdateSellingPlanRequest request) {
        return adminService.updateSellingPlan(request);
    }

    @DeleteMapping("/plans")
    public LinkProductResponse removePlan(@RequestBody RemoveSellingPlanRequest request) {
        return adminService.removeSellingPlanFromGroup(request);
    }
}
