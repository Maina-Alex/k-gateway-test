package com.ekenya.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alex Maina
 * @created 17/03/2023
 **/
@Configuration
public class GatewayRoutesConfig {

    private static final String REWRITEPATH = "/channel(?<remainingPath>/.*)$";
    private static final String REPLACEMENT= "/${remainingPath}";
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authService", r -> r.path("/channel/oauth/**","/oauth/**")
                        .filters(f -> f.rewritePath(REWRITEPATH, REPLACEMENT))
                        .uri("lb://AUTH-SERVICE"))
                .route("user-service", r -> r.path("/channel/api/v1/admin/**","/api/v1/admin/**")
                        .filters(f -> f.rewritePath(REWRITEPATH, REPLACEMENT))
                        .uri("lb://USER-SERVICE"))
                .route("product-service", r -> r.path("/channel/product/**","/product/**")
                        .filters(f -> f.rewritePath(REWRITEPATH, REPLACEMENT))
                        .uri("lb://PRODUCT-SERVICE"))
                .build();
    }
}
