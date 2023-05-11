package com.karaniDev.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

/**
 * @author Alex Maina
 * @created 17/03/2023
 **/
@Configuration
public class GatewayRoutesConfig {
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("elderlyService", r -> r.path("/api/v1/elderly/**","/api/v1/worker/**")
                        .filters(f -> {
                            f.addResponseHeader ("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                            return f;
                        })
                        .uri("lb://elderly-service"))
                .route("authService", r -> r.path("/users/**","/auth/**")
                        .filters(f -> {
                            f.addResponseHeader ("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                            return f;
                        })
                        .uri("lb://AUTH-SERVICE"))

                .build();
    }
}
