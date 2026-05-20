package com.example.membership_flow.subscription;

import com.example.membership_flow.shopify.ShopifyAdminClient;
import com.example.membership_flow.shopify.ShopifyProperties;
import com.example.membership_flow.shopify.graphql.GraphQLRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String LIST_SUBSCRIPTION_PRODUCTS = """
            query {
              sellingPlanGroups(first: 50) {
                edges {
                  node {
                    id
                    name
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
                    products(first: 50) {
                      edges {
                        node {
                          id
                          title
                          status
                          featuredImage { url altText }
                          variants(first: 1) {
                            edges { node { id price } }
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
    private final ShopifyProperties shopifyProperties;

    public SubscriptionProductsResponse listSubscriptionProducts() {
        var result = shopifyAdminClient.listSubscriptionProducts(
                new GraphQLRequest(LIST_SUBSCRIPTION_PRODUCTS, null));

        if (result.data() == null) {
            throw new IllegalStateException("Shopify returned no data for subscription products query");
        }

        // Build product-centric map: productId -> entry with all groups/plans accumulated
        Map<String, ProductAccumulator> productMap = new LinkedHashMap<>();

        for (var groupEdge : result.data().sellingPlanGroups().edges()) {
            var group = groupEdge.node();

            // Build plan entries for this group
            var plans = group.sellingPlans().edges().stream()
                    .map(pe -> {
                        var plan = pe.node();
                        var billing = plan.billingPolicy();
                        var interval = billing != null ? billing.interval() : null;
                        int intervalCount = (billing != null && billing.intervalCount() != null) ? billing.intervalCount() : 1;
                        Double discount = null;
                        if (plan.pricingPolicies() != null && !plan.pricingPolicies().isEmpty()) {
                            var av = plan.pricingPolicies().getFirst().adjustmentValue();
                            if (av != null) discount = av.percentage();
                        }
                        return new SubscriptionProductsResponse.PlanEntry(
                                plan.id(), plan.name(), interval, intervalCount, discount, null); // checkout_url set below
                    })
                    .toList();

            for (var productEdge : group.products().edges()) {
                var product = productEdge.node();
                if (!"ACTIVE".equals(product.status())) continue;

                var acc = productMap.computeIfAbsent(product.id(), id -> {
                    var variant = (product.variants() != null && !product.variants().edges().isEmpty())
                            ? product.variants().edges().getFirst().node() : null;
                    var image = product.featuredImage();
                    return new ProductAccumulator(
                            product.id(), product.title(), product.status(),
                            variant != null ? variant.id() : null,
                            variant != null ? variant.price() : null,
                            image != null ? image.url() : null,
                            image != null ? image.altText() : null
                    );
                });

                // Build checkout URLs for each plan using this product's variant
                var variantNumericId = acc.variantId != null ? numericId(acc.variantId) : null;
                var plansWithUrls = plans.stream().map(p -> {
                    String url = null;
                    if (variantNumericId != null) {
                        url = "https://%s/cart/add?id=%s&quantity=1&selling_plan=%s&return_to=%%2Fcheckout"
                                .formatted(shopifyProperties.storeDomain(), variantNumericId, numericId(p.planId()));
                    }
                    return new SubscriptionProductsResponse.PlanEntry(
                            p.planId(), p.planName(), p.interval(), p.intervalCount(), p.discountPercentage(), url);
                }).toList();

                acc.groups.add(new SubscriptionProductsResponse.GroupEntry(group.id(), group.name(), plansWithUrls));
            }
        }

        var products = productMap.values().stream()
                .map(acc -> new SubscriptionProductsResponse.ProductEntry(
                        acc.id, acc.title, acc.status, acc.variantId, acc.price,
                        acc.imageUrl, acc.imageAlt, acc.groups))
                .toList();

        return new SubscriptionProductsResponse(products.size(), products);
    }

    private static String numericId(String gid) {
        return gid.substring(gid.lastIndexOf('/') + 1);
    }

    /** Mutable accumulator used while building the product map. */
    private static class ProductAccumulator {
        final String id, title, status, variantId, price, imageUrl, imageAlt;
        final List<SubscriptionProductsResponse.GroupEntry> groups = new ArrayList<>();

        ProductAccumulator(String id, String title, String status, String variantId,
                           String price, String imageUrl, String imageAlt) {
            this.id = id; this.title = title; this.status = status;
            this.variantId = variantId; this.price = price;
            this.imageUrl = imageUrl; this.imageAlt = imageAlt;
        }
    }
}
