package com.example.membership_flow.shopify;

import com.example.membership_flow.shopify.token.ShopifyTokenClient;
import com.example.membership_flow.shopify.token.ShopifyTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

@ImportHttpServices(group = "shopify",      types = ShopifyAdminClient.class)
@ImportHttpServices(group = "shopify-auth", types = ShopifyTokenClient.class)
@Configuration
@EnableConfigurationProperties(ShopifyProperties.class)
public class ShopifyClientConfig {
    private final String X_SHOPIFY_ACCESS_TOKEN = "X-Shopify-Access-Token";
    /**
     * "shopify-auth" group — token endpoint only, no auth header needed.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer shopifyAuthConfigurer(ShopifyProperties props) {
        return groups -> groups.filterByName("shopify-auth").forEachClient((_, builder) ->
                builder.baseUrl("https://" + props.storeDomain())
        );
    }

    /**
     * "shopify" group — Admin GraphQL API.
     * Token is fetched lazily on the first request and cached inside ShopifyTokenService.
     * @Lazy breaks the potential init-order cycle: ShopifyAdminClient proxy → this configurer
     *        → ShopifyTokenService → ShopifyTokenClient proxy → shopifyAuthConfigurer.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer shopifyAdminConfigurer(
            ShopifyProperties props,
            @Lazy @Autowired ShopifyTokenService tokenService) {

        return groups -> groups.filterByName("shopify").forEachClient((_, builder) ->
                builder.baseUrl("https://%s/admin/api/%s".formatted(props.storeDomain(), props.apiVersion()))
                       .requestInterceptor((request, body, execution) -> {
                           request.getHeaders().set(X_SHOPIFY_ACCESS_TOKEN, tokenService.getAccessToken());
                           return execution.execute(request, body);
                       })
        );
    }
}
