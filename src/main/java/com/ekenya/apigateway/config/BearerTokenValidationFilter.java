package com.ekenya.apigateway.config;

import com.ekenya.apigateway.model.UniversalResponse;
import com.ekenya.apigateway.security.JwtUtilService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Alex Maina
 * @created 13/03/2023
 **/
@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(4)
public class BearerTokenValidationFilter implements WebFilter {
    @Value ("${login.endpoints.validate}")
    private String tokenValidationEndpoint;
    private final JwtUtilService jwtUtilService;
    private final WebClient.Builder loadBalancedWebClientBuilder;
    private final Gson gson;
    private static final String BEARER = "Bearer ";
    private static final Predicate<String> matchBearerLength = authValue -> authValue.length () > BEARER.length ();
    private static final Function<String, Mono<String>> isolateBearerValue = authValue -> Mono.justOrEmpty (authValue.substring (BEARER.length ()));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty (exchange)
                .flatMap (BearerTokenValidationFilter::extract)
                .filter (matchBearerLength)
                .flatMap (isolateBearerValue)
                .flatMap (bearerToken ->
                        loadBalancedWebClientBuilder.build ()
                                .post ()
                                .uri (tokenValidationEndpoint)
                                .body (Mono.just (Map.of ("token", bearerToken)), Object.class)
                                .retrieve ()
                                .bodyToMono (UniversalResponse.class)
                                .onErrorResume (err -> {
                                    log.error ("An error occurred validating token", err);
                                    return Mono.just (UniversalResponse.builder ().status (400).message ("Failed to validate session.").build ());
                                })
                                .flatMap (res -> {
                                    if (res.getStatus () == 400) {
                                        DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory ()
                                                .wrap (gson.toJson (UniversalResponse.builder ()
                                                        .status (400).message ("Failed to validate session.").build ()).getBytes ());
                                        ServerHttpResponse response = exchange.getResponse ();
                                        response.setStatusCode (HttpStatus.UNAUTHORIZED);
                                        response.getHeaders ().setContentType (MediaType.APPLICATION_JSON);
                                        return exchange.getResponse ().writeWith (Mono.just (bodyDataBuffer))
                                                .flatMap (exc -> exchange.getResponse ().setComplete ());
                                    } else {
                                        Map<String, Object> result = (Map<String, Object>) res.getData ();
                                        String userName = (String) result.get ("username");
                                        List<String> roles = (List<String>) result.get ("roles");
                                        List<String> authorities = (List<String>) result.get ("permissions");
                                        String internalToken = jwtUtilService.generateJwt (userName, roles, authorities);
                                        exchange.getRequest ().getHeaders ().set (HttpHeaders.AUTHORIZATION, internalToken);
                                        return chain.filter (exchange);
                                    }
                                }))
                .switchIfEmpty (chain.filter (exchange));
    }

    public static Mono<String> extract(ServerWebExchange serverWebExchange) {
        return Mono.justOrEmpty (serverWebExchange.getRequest ()
                .getHeaders ()
                .getFirst (HttpHeaders.AUTHORIZATION));
    }
}
