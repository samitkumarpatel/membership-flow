package com.example.membership_flow.admin;

import com.example.membership_flow.admin.dto.ProductsResponse;
import com.example.membership_flow.billing.BillingAttemptSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/products")
    public ProductsResponse listProducts() {
        return adminService.listProducts();
    }

    @GetMapping("/billing-attempts")
    public List<BillingAttemptSummary> listBillingAttempts(
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) String status) {
        return adminService.listBillingAttempts(contractId, status);
    }
}
