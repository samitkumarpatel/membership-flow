package com.example.membership_flow.admin;

import com.example.membership_flow.admin.dto.ActivateContractRequest;
import com.example.membership_flow.admin.dto.BillingAttemptRequest;
import com.example.membership_flow.admin.dto.BillingAttemptResponse;
import com.example.membership_flow.admin.dto.CancelContractRequest;
import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.admin.dto.PauseContractRequest;
import com.example.membership_flow.admin.dto.SubscriptionContractsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/subscription-contracts")
@RequiredArgsConstructor
public class SubscriptionContractController {

    private final AdminService adminService;

    // ── Contracts ─────────────────────────────────────────────────────────────

    @GetMapping
    public SubscriptionContractsResponse list() {
        return adminService.listSubscriptionContracts();
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @PatchMapping("/cancel")
    public LinkProductResponse cancel(@RequestBody CancelContractRequest request) {
        return adminService.cancelSubscriptionContract(request);
    }

    @PatchMapping("/pause")
    public LinkProductResponse pause(@RequestBody PauseContractRequest request) {
        return adminService.pauseSubscriptionContract(request);
    }

    @PatchMapping("/activate")
    public LinkProductResponse activate(@RequestBody ActivateContractRequest request) {
        return adminService.activateSubscriptionContract(request);
    }

    // ── Billing attempts ──────────────────────────────────────────────────────

    @PostMapping("/billing-attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingAttemptResponse createBillingAttempt(@RequestBody BillingAttemptRequest request) {
        return adminService.createBillingAttempt(request);
    }

    @GetMapping("/billing-attempts")
    public BillingAttemptResponse getBillingAttempt(@RequestParam String attemptId) {
        return adminService.getBillingAttempt(attemptId);
    }
}
