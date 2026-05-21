package com.example.membership_flow.customer;

import com.example.membership_flow.admin.AdminService;
import com.example.membership_flow.admin.ShopifyUserErrorException;
import com.example.membership_flow.admin.dto.ActivateContractRequest;
import com.example.membership_flow.admin.dto.CancelContractRequest;
import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.admin.dto.PauseContractRequest;
import com.example.membership_flow.billing.BillingAttemptInfo;
import com.example.membership_flow.customer.dto.CustomerLookupRequest;
import com.example.membership_flow.customer.dto.CustomerSubscriptionsResponse;
import com.example.membership_flow.shopify.ShopifyAdminClient;
import com.example.membership_flow.shopify.graphql.CustomerSubscriptionContractsQueryResult.CustomerNode;
import com.example.membership_flow.shopify.graphql.GraphQLRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerSubscriptionService {

    private static final String FIND_CUSTOMER_CONTRACTS = """
            query findCustomerContracts($query: String!) {
              customers(first: 1, query: $query) {
                edges {
                  node {
                    id
                    email
                    phone
                    firstName
                    lastName
                    subscriptionContracts(first: 20) {
                      edges {
                        node {
                          id
                          status
                          nextBillingDate
                          createdAt
                          billingPolicy {
                            interval
                            intervalCount
                          }
                          billingAttempts(first: 10, reverse: true) {
                            edges {
                              node {
                                id
                                ready
                                errorCode
                                errorMessage
                                createdAt
                                order {
                                  id
                                  name
                                }
                              }
                            }
                          }
                          lines(first: 5) {
                            edges {
                              node {
                                id
                                title
                                quantity
                                sellingPlanName
                                sellingPlanId
                                currentPrice {
                                  amount
                                  currencyCode
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private final ShopifyAdminClient shopifyAdminClient;
    private final AdminService adminService;

    public CustomerSubscriptionsResponse lookup(CustomerLookupRequest request) {
        String queryStr;
        if (request.email() != null && !request.email().isBlank()) {
            queryStr = "email:" + request.email().trim();
        } else if (request.phone() != null && !request.phone().isBlank()) {
            queryStr = "phone:" + request.phone().trim();
        } else {
            throw new IllegalArgumentException("Provide either email or phone");
        }

        var result = shopifyAdminClient.findCustomerContracts(
                new GraphQLRequest(FIND_CUSTOMER_CONTRACTS, Map.of("query", queryStr)));

        if (result.data() == null || result.data().customers() == null
                || result.data().customers().edges().isEmpty()) {
            return new CustomerSubscriptionsResponse(null, null, null, null, 0, List.of());
        }

        CustomerNode customer = result.data().customers().edges().getFirst().node();

        var contracts = (customer.subscriptionContracts() == null
                ? List.<com.example.membership_flow.shopify.graphql.CustomerSubscriptionContractsQueryResult.ContractEdge>of()
                : customer.subscriptionContracts().edges())
                .stream()
                .map(ce -> {
                    var c = ce.node();
                    var billing = c.billingPolicy() != null
                            ? new CustomerSubscriptionsResponse.BillingPolicy(
                                    c.billingPolicy().interval(),
                                    c.billingPolicy().intervalCount() != null ? c.billingPolicy().intervalCount() : 1)
                            : null;
                    var lines = c.lines() == null ? List.<CustomerSubscriptionsResponse.LineItem>of()
                            : c.lines().edges().stream()
                                    .map(le -> {
                                        var ln = le.node();
                                        var price = ln.currentPrice() != null ? ln.currentPrice().amount() : null;
                                        var currency = ln.currentPrice() != null ? ln.currentPrice().currencyCode() : null;
                                        return new CustomerSubscriptionsResponse.LineItem(
                                                ln.title(), ln.quantity() != null ? ln.quantity() : 1,
                                                ln.sellingPlanName(), ln.sellingPlanId(), price, currency);
                                    })
                                    .toList();
                    var attempts = c.billingAttempts() == null ? List.<BillingAttemptInfo>of()
                            : c.billingAttempts().edges().stream()
                                    .map(ae -> {
                                        var a = ae.node();
                                        var orderId = a.order() != null ? a.order().id() : null;
                                        var orderName = a.order() != null ? a.order().name() : null;
                                        return new BillingAttemptInfo(
                                                a.id(), a.ready(), a.errorCode(), a.errorMessage(),
                                                a.createdAt(), orderId, orderName);
                                    })
                                    .toList();
                    return new CustomerSubscriptionsResponse.ContractItem(
                            c.id(), c.status(), c.nextBillingDate(), c.createdAt(), billing, lines, attempts);
                })
                .toList();

        return new CustomerSubscriptionsResponse(
                customer.id(), customer.email(),
                customer.firstName(), customer.lastName(),
                contracts.size(), contracts);
    }

    public LinkProductResponse pause(String contractId) {
        return adminService.pauseSubscriptionContract(new PauseContractRequest(contractId));
    }

    public LinkProductResponse resume(String contractId) {
        return adminService.activateSubscriptionContract(new ActivateContractRequest(contractId));
    }

    public LinkProductResponse cancel(String contractId) {
        return adminService.cancelSubscriptionContract(new CancelContractRequest(contractId));
    }
}
