package com.ekenya.apigateway.config;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route(p -> p.path("/api/v1/oauth/**")
                        .uri("lb://auth-server"))
                .route(p -> p.path("/api/v1/corporate/**","/api/v1/mpesa/**")
                        .uri("lb://corporate-service"))
                .route(p -> p.path("/api/v1/employee/**")
                        .uri("lb://employee-service"))
                .route(p -> p.path("/api/v1/bank/**")
                        .uri("lb://bank-service"))
                .build();
    }
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig
                        .custom()
                        .minimumNumberOfCalls(1)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(1)
                        .waitDurationInOpenState(Duration.ofMillis(100))
                        .slowCallDurationThreshold(Duration.ofMinutes(1))
                        .slowCallRateThreshold(1f)
                        .failureRateThreshold(1f)
                        .build()).timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build())
                .build());
    }

    @Bean
    @SuppressWarnings({"deprecated"})
    public ReactiveResilience4JCircuitBreakerFactory factory() {
        return new ReactiveResilience4JCircuitBreakerFactory();
    }
}
