package com.example.membership_flow.shopify;

import com.example.membership_flow.shopify.graphql.GraphQLRequest;
import com.example.membership_flow.shopify.graphql.ProductsQueryResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupAddProductsResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupCreateResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupDeleteResult;
import com.example.membership_flow.shopify.graphql.CustomerSubscriptionContractsQueryResult;
import com.example.membership_flow.shopify.graphql.SubscriptionContractActivateResult;
import com.example.membership_flow.shopify.graphql.SubscriptionContractCancelResult;
import com.example.membership_flow.shopify.graphql.SubscriptionContractPauseResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupProductsQueryResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupRemoveProductsResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupUpdateResult;
import com.example.membership_flow.shopify.graphql.SellingPlanGroupsQueryResult;
import com.example.membership_flow.shopify.graphql.SubscriptionContractsQueryResult;
import com.example.membership_flow.shopify.graphql.SubscriptionProductsQueryResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Shopify Admin GraphQL HTTP service interface.
 * All methods POST to /graphql.json — Spring deserializes each into its declared return type.
 * Base URL and X-Shopify-Access-Token header are wired via ShopifyClientConfig.
 */
public interface ShopifyAdminClient {

    @PostExchange("/graphql.json")
    SellingPlanGroupsQueryResult listSellingPlanGroups(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupProductsQueryResult getSellingPlanGroupProducts(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupCreateResult createSellingPlanGroup(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupAddProductsResult addProductsToGroup(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupRemoveProductsResult removeProductsFromGroup(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    ProductsQueryResult listProducts(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupUpdateResult updateSellingPlanGroup(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionContractsQueryResult listSubscriptionContracts(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionProductsQueryResult listSubscriptionProducts(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SellingPlanGroupDeleteResult deleteSellingPlanGroup(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionContractCancelResult cancelSubscriptionContract(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionContractPauseResult pauseSubscriptionContract(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionContractActivateResult activateSubscriptionContract(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    CustomerSubscriptionContractsQueryResult findCustomerContracts(@RequestBody GraphQLRequest request);
}
