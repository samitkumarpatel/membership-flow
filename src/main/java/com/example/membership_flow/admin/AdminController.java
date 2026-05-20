package com.example.membership_flow.admin;

import com.example.membership_flow.admin.dto.AddSellingPlanRequest;
import com.example.membership_flow.admin.dto.CheckoutUrlRequest;
import com.example.membership_flow.admin.dto.CheckoutUrlResponse;
import com.example.membership_flow.admin.dto.SubscriptionContractsResponse;
import com.example.membership_flow.admin.dto.CreateSellingGroupRequest;
import com.example.membership_flow.admin.dto.CreateSellingGroupResponse;
import com.example.membership_flow.admin.dto.GroupProductsResponse;
import com.example.membership_flow.admin.dto.LinkProductRequest;
import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.admin.dto.ProductsResponse;
import com.example.membership_flow.admin.dto.RemoveProductRequest;
import com.example.membership_flow.admin.dto.SellingGroupsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/selling-groups")
    public SellingGroupsResponse listSellingGroups() {
        return adminService.listSellingGroups();
    }

    @GetMapping("/selling-groups/products")
    public GroupProductsResponse getGroupProducts(@RequestParam String groupId) {
        return adminService.getGroupProducts(groupId);
    }

    @PostMapping("/create-selling-group")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSellingGroupResponse createSellingGroup(@RequestBody CreateSellingGroupRequest request) {
        return adminService.createSellingGroup(request);
    }

    @PostMapping("/link-product-to-plan")
    public LinkProductResponse linkProductToPlan(@RequestBody LinkProductRequest request) {
        return adminService.linkProductToPlan(request);
    }

    @PostMapping("/remove-products-from-plan")
    public LinkProductResponse removeProductsFromPlan(@RequestBody RemoveProductRequest request) {
        return adminService.removeProductsFromGroup(request);
    }

    @GetMapping("/products")
    public ProductsResponse listProducts() {
        return adminService.listProducts();
    }

    @PostMapping("/selling-groups/add-plan")
    public LinkProductResponse addSellingPlan(@RequestBody AddSellingPlanRequest request) {
        return adminService.addSellingPlanToGroup(request);
    }

    @PostMapping("/checkout-url")
    public CheckoutUrlResponse buildCheckoutUrl(@RequestBody CheckoutUrlRequest request) {
        return adminService.buildCheckoutUrl(request);
    }

    @GetMapping("/subscription-contracts")
    public SubscriptionContractsResponse listSubscriptionContracts() {
        return adminService.listSubscriptionContracts();
    }
}
