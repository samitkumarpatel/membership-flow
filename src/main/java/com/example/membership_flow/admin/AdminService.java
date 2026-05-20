package com.example.membership_flow.admin;

import com.example.membership_flow.admin.dto.AddSellingPlanRequest;
import com.example.membership_flow.admin.dto.CheckoutUrlRequest;
import com.example.membership_flow.admin.dto.SubscriptionContractsResponse;
import com.example.membership_flow.admin.dto.CheckoutUrlResponse;
import com.example.membership_flow.admin.dto.CreateSellingGroupRequest;
import com.example.membership_flow.admin.dto.CreateSellingGroupResponse;
import com.example.membership_flow.admin.dto.GroupProductsResponse;
import com.example.membership_flow.admin.dto.LinkProductRequest;
import com.example.membership_flow.admin.dto.LinkProductResponse;
import com.example.membership_flow.admin.dto.ProductsResponse;
import com.example.membership_flow.admin.dto.RemoveProductRequest;
import com.example.membership_flow.admin.dto.RemoveSellingPlanRequest;
import com.example.membership_flow.admin.dto.SellingGroupsResponse;
import com.example.membership_flow.shopify.ShopifyAdminClient;
import com.example.membership_flow.shopify.ShopifyProperties;
import com.example.membership_flow.shopify.graphql.GraphQLRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String LIST_SELLING_PLAN_GROUPS = """
            query {
              sellingPlanGroups(first: 50) {
                edges {
                  node {
                    id
                    name
                    merchantCode
                    productsCount { count }
                    sellingPlans(first: 20) {
                      edges {
                        node {
                          id
                          name
                          billingPolicy {
                            ... on SellingPlanRecurringBillingPolicy {
                              interval
                              intervalCount
                            }
                          }
                          pricingPolicies {
                            ... on SellingPlanFixedPricingPolicy {
                              adjustmentType
                              adjustmentValue {
                                ... on SellingPlanPricingPolicyPercentageValue {
                                  percentage
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

    private static final String CREATE_SELLING_PLAN_GROUP = """
            mutation sellingPlanGroupCreate($input: SellingPlanGroupInput!, $resources: SellingPlanGroupResourceInput) {
              sellingPlanGroupCreate(input: $input, resources: $resources) {
                sellingPlanGroup {
                  id
                  sellingPlans(first: 10) {
                    edges { node { id } }
                  }
                }
                userErrors { field message }
              }
            }
            """;

    private static final String GET_SELLING_PLAN_GROUP_PRODUCTS = """
            query getSellingPlanGroupProducts($id: ID!) {
              sellingPlanGroup(id: $id) {
                id
                name
                products(first: 50) {
                  edges {
                    node {
                      id
                      title
                      status
                      variants(first: 1) {
                        edges { node { id } }
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String REMOVE_PRODUCTS_FROM_GROUP = """
            mutation sellingPlanGroupRemoveProducts($id: ID!, $productIds: [ID!]!) {
              sellingPlanGroupRemoveProducts(id: $id, productIds: $productIds) {
                removedProductIds
                userErrors { field message }
              }
            }
            """;

    private static final String ADD_PRODUCTS_TO_GROUP = """
            mutation sellingPlanGroupAddProducts($id: ID!, $productIds: [ID!]!) {
              sellingPlanGroupAddProducts(id: $id, productIds: $productIds) {
                sellingPlanGroup { id }
                userErrors { field message }
              }
            }
            """;

    private static final String LIST_PRODUCTS = """
            query getProducts($after: String) {
              products(first: 250, after: $after) {
                pageInfo { hasNextPage endCursor }
                edges {
                  node {
                    id
                    title
                    status
                    variants(first: 1) {
                      edges { node { id } }
                    }
                  }
                }
              }
            }
            """;

    private static final String LIST_SUBSCRIPTION_CONTRACTS = """
            query {
              subscriptionContracts(first: 20) {
                edges {
                  node {
                    id
                    status
                    nextBillingDate
                    createdAt
                    customer {
                      id
                      email
                      firstName
                      lastName
                    }
                    billingPolicy {
                      interval
                      intervalCount
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
                    orders(first: 3) {
                      edges {
                        node {
                          id
                          name
                          createdAt
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String UPDATE_SELLING_PLAN_GROUP = """
            mutation sellingPlanGroupUpdate($id: ID!, $input: SellingPlanGroupInput!) {
              sellingPlanGroupUpdate(id: $id, input: $input) {
                sellingPlanGroup { id }
                userErrors { field message }
              }
            }
            """;

    private static final String DELETE_SELLING_PLAN_GROUP = """
            mutation sellingPlanGroupDelete($id: ID!) {
              sellingPlanGroupDelete(id: $id) {
                deletedSellingPlanGroupId
                userErrors { field message }
              }
            }
            """;

    private final ShopifyAdminClient shopifyAdminClient;
    private final ShopifyProperties shopifyProperties;

    public SellingGroupsResponse listSellingGroups() {
        var result = shopifyAdminClient.listSellingPlanGroups(new GraphQLRequest(LIST_SELLING_PLAN_GROUPS, null));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroups query");
        }

        var items = result.data().sellingPlanGroups().edges().stream()
                .map(e -> {
                    var node = e.node();
                    var plans = node.sellingPlans().edges().stream()
                            .map(se -> {
                                var plan = se.node();
                                var billing      = plan.billingPolicy();
                                var interval     = billing != null ? billing.interval() : null;
                                int intervalCount = (billing != null && billing.intervalCount() != null)
                                        ? billing.intervalCount() : 0;
                                Double discount  = null;
                                if (plan.pricingPolicies() != null && !plan.pricingPolicies().isEmpty()) {
                                    var av = plan.pricingPolicies().getFirst().adjustmentValue();
                                    if (av != null) discount = av.percentage();
                                }
                                return new SellingGroupsResponse.SellingPlanItem(
                                        plan.id(), plan.name(), interval, intervalCount, discount);
                            })
                            .toList();

                    int productsCount = node.productsCount() != null && node.productsCount().count() != null
                            ? node.productsCount().count() : 0;

                    return new SellingGroupsResponse.SellingGroupItem(
                            node.id(), node.name(), node.merchantCode(), productsCount, plans);
                })
                .toList();

        return new SellingGroupsResponse(items);
    }

    public GroupProductsResponse getGroupProducts(String groupId) {
        var result = shopifyAdminClient.getSellingPlanGroupProducts(
                new GraphQLRequest(GET_SELLING_PLAN_GROUP_PRODUCTS, Map.of("id", groupId)));

        if (result.data() == null || result.data().sellingPlanGroup() == null) {
            throw new IllegalArgumentException("Selling plan group not found: " + groupId);
        }

        var group = result.data().sellingPlanGroup();
        var products = group.products().edges().stream()
                .map(e -> {
                    var node = e.node();
                    var variantId = (node.variants() != null && !node.variants().edges().isEmpty())
                            ? node.variants().edges().getFirst().node().id() : null;
                    return new GroupProductsResponse.ProductItem(node.id(), node.title(), node.status(), variantId);
                })
                .toList();

        return new GroupProductsResponse(group.id(), group.name(), products.size(), products);
    }

    public LinkProductResponse removeProductsFromGroup(RemoveProductRequest request) {
        var variables = Map.of("id", request.sellingPlanGroupId(), "productIds", request.productIds());
        var result = shopifyAdminClient.removeProductsFromGroup(
                new GraphQLRequest(REMOVE_PRODUCTS_FROM_GROUP, variables));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupRemoveProducts");
        }

        var payload = result.data().sellingPlanGroupRemoveProducts();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        return new LinkProductResponse("success", "Removed %d products".formatted(request.productIds().size()));
    }

    public CreateSellingGroupResponse createSellingGroup(CreateSellingGroupRequest request) {
        var plans = request.sellingPlans() == null || request.sellingPlans().isEmpty()
                ? List.<Object>of()
                : request.sellingPlans().stream().map(this::buildSellingPlanInput).toList();

        var variables = Map.of(
                "input", Map.of(
                        "name", request.name(),
                        "merchantCode", toSlug(request.name()),
                        "options", List.of("Subscription"),
                        "sellingPlansToCreate", plans
                )
        );

        var result = shopifyAdminClient.createSellingPlanGroup(new GraphQLRequest(CREATE_SELLING_PLAN_GROUP, variables));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupCreate");
        }

        var payload = result.data().sellingPlanGroupCreate();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        var group = payload.sellingPlanGroup();
        var planIds = group.sellingPlans().edges().stream()
                .map(e -> e.node().id())
                .toList();

        return new CreateSellingGroupResponse("success", group.id(), planIds);
    }

    public LinkProductResponse linkProductToPlan(LinkProductRequest request) {
        var variables = Map.of(
                "id", request.sellingPlanGroupId(),
                "productIds", request.productIds()
        );

        var result = shopifyAdminClient.addProductsToGroup(new GraphQLRequest(ADD_PRODUCTS_TO_GROUP, variables));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupAddProducts");
        }

        var payload = result.data().sellingPlanGroupAddProducts();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        return new LinkProductResponse("success", "Linked to %d products".formatted(request.productIds().size()));
    }

    public ProductsResponse listProducts() {
        var allItems = new java.util.ArrayList<ProductsResponse.ProductItem>();
        String cursor = null;

        do {
            var variables = new java.util.HashMap<String, Object>();
            variables.put("after", cursor);
            var result = shopifyAdminClient.listProducts(new GraphQLRequest(LIST_PRODUCTS, variables));

            if (result.data() == null) {
                throw new IllegalStateException("Shopify returned no data for products query");
            }

            var connection = result.data().products();
            connection.edges().forEach(e -> {
                var node = e.node();
                var variantId = (node.variants() != null && !node.variants().edges().isEmpty())
                        ? node.variants().edges().getFirst().node().id() : null;
                allItems.add(new ProductsResponse.ProductItem(node.id(), node.title(), node.status(), variantId));
            });

            var pageInfo = connection.pageInfo();
            cursor = (pageInfo != null && pageInfo.hasNextPage()) ? pageInfo.endCursor() : null;
        } while (cursor != null);

        return new ProductsResponse(allItems.size(), allItems);
    }

    public LinkProductResponse addSellingPlanToGroup(AddSellingPlanRequest request) {
        var plans = request.sellingPlans() == null || request.sellingPlans().isEmpty()
                ? List.<Object>of()
                : request.sellingPlans().stream().map(this::buildSellingPlanInput).toList();

        var variables = Map.of(
                "id", request.sellingPlanGroupId(),
                "input", Map.of("sellingPlansToCreate", plans)
        );

        var result = shopifyAdminClient.updateSellingPlanGroup(
                new GraphQLRequest(UPDATE_SELLING_PLAN_GROUP, variables));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupUpdate");
        }

        var payload = result.data().sellingPlanGroupUpdate();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        return new LinkProductResponse("success",
                "Added %d selling plan(s)".formatted(request.sellingPlans().size()));
    }

    public SubscriptionContractsResponse listSubscriptionContracts() {
        var result = shopifyAdminClient.listSubscriptionContracts(
                new GraphQLRequest(LIST_SUBSCRIPTION_CONTRACTS, null));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for subscriptionContracts query");
        }

        var contracts = result.data().subscriptionContracts().edges().stream()
                .map(e -> {
                    var n = e.node();

                    var customer = n.customer() != null
                            ? new SubscriptionContractsResponse.CustomerInfo(
                                    n.customer().id(), n.customer().email(),
                                    n.customer().firstName(), n.customer().lastName())
                            : null;

                    var billing = n.billingPolicy() != null
                            ? new SubscriptionContractsResponse.BillingPolicy(
                                    n.billingPolicy().interval(), n.billingPolicy().intervalCount())
                            : null;

                    var lines = n.lines() == null ? List.<SubscriptionContractsResponse.LineItem>of()
                            : n.lines().edges().stream()
                                    .map(le -> {
                                        var ln = le.node();
                                        var price = ln.currentPrice() != null ? ln.currentPrice().amount() : null;
                                        var currency = ln.currentPrice() != null ? ln.currentPrice().currencyCode() : null;
                                        return new SubscriptionContractsResponse.LineItem(
                                                ln.title(), ln.quantity(), ln.sellingPlanName(),
                                                ln.sellingPlanId(), price, currency);
                                    })
                                    .toList();

                    var orders = n.orders() == null ? List.<SubscriptionContractsResponse.OrderRef>of()
                            : n.orders().edges().stream()
                                    .map(oe -> new SubscriptionContractsResponse.OrderRef(
                                            oe.node().id(), oe.node().name(), oe.node().createdAt()))
                                    .toList();

                    return new SubscriptionContractsResponse.ContractItem(
                            n.id(), n.status(), n.nextBillingDate(), n.createdAt(),
                            customer, billing, lines, orders);
                })
                .toList();

        return new SubscriptionContractsResponse(contracts.size(), contracts);
    }

    public LinkProductResponse removeSellingPlanFromGroup(RemoveSellingPlanRequest request) {
        var variables = Map.of(
                "id", request.sellingPlanGroupId(),
                "input", Map.of("sellingPlansToDelete", request.sellingPlanIds())
        );

        var result = shopifyAdminClient.updateSellingPlanGroup(
                new GraphQLRequest(UPDATE_SELLING_PLAN_GROUP, variables));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupUpdate");
        }

        var payload = result.data().sellingPlanGroupUpdate();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        return new LinkProductResponse("success",
                "Removed %d selling plan(s)".formatted(request.sellingPlanIds().size()));
    }

    public LinkProductResponse deleteSellingGroup(String groupId) {
        var result = shopifyAdminClient.deleteSellingPlanGroup(
                new GraphQLRequest(DELETE_SELLING_PLAN_GROUP, Map.of("id", groupId)));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for sellingPlanGroupDelete");
        }

        var payload = result.data().sellingPlanGroupDelete();
        if (!payload.userErrors().isEmpty()) {
            throw new ShopifyUserErrorException(payload.userErrors());
        }

        return new LinkProductResponse("success", "Deleted selling plan group: " + groupId);
    }

    public CheckoutUrlResponse buildCheckoutUrl(CheckoutUrlRequest request) {
        var variantNumericId = numericId(request.variantId());
        var planNumericId = numericId(request.sellingPlanId());
        var url = "https://%s/cart/add?id=%s&quantity=1&selling_plan=%s"
                .formatted(shopifyProperties.storeDomain(), variantNumericId, planNumericId);
        return new CheckoutUrlResponse(url, request.variantId(), request.sellingPlanId());
    }

    private static String numericId(String gid) {
        return gid.substring(gid.lastIndexOf('/') + 1);
    }

    private Map<String, Object> buildSellingPlanInput(CreateSellingGroupRequest.SellingPlanInput plan) {
        var intervalLabel = plan.interval().charAt(0) + plan.interval().substring(1).toLowerCase();
        var pricingPolicies = plan.discountPercentage() > 0
                ? List.of(Map.of("fixed", Map.of(
                        "adjustmentType", "PERCENTAGE",
                        "adjustmentValue", Map.of("percentage", plan.discountPercentage()))))
                : List.<Object>of();

        return Map.of(
                "name", "%s (%d%% off)".formatted(intervalLabel, (int) plan.discountPercentage()),
                "options", intervalLabel,
                "category", "SUBSCRIPTION",
                "billingPolicy", Map.of("recurring", Map.of(
                        "interval", plan.interval(),
                        "intervalCount", plan.intervalCount()
                )),
                "deliveryPolicy", Map.of("recurring", Map.of(
                        "interval", plan.interval(),
                        "intervalCount", plan.intervalCount()
                )),
                "pricingPolicies", pricingPolicies
        );
    }

    private static String toSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
