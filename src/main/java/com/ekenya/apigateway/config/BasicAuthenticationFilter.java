package com.ekenya.apigateway.config;

import com.ekenya.apigateway.model.UniversalResponse;
import com.ekenya.apigateway.security.JwtUtilService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Alex Maina
 * @created 13/03/2023
 * <p>Authenticates all clients as integrators ->
 *     Transforms basic authentication to bearer authentication with default role integrator</p>
 **/
@RequiredArgsConstructor
@Configuration
@Order(5)
public class BasicAuthenticationFilter implements WebFilter {
    @Value("${services.integration.basic.client}")
    private String clientSecret;
    @Value("${services.integration.basic.password}")
    private String clientPassword;
    private final Gson gson;
    private final JwtUtilService jwtUtilService;
    private static final String BASIC = "Basic ";
    private static final Predicate<String> matchBasicLength = authValue -> authValue.length () > BASIC.length ();
    private static final Function<String, Mono<String>> isolateBasicValue = authValue -> Mono.justOrEmpty (authValue.substring (BASIC.length ()));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty (exchange)
                .flatMap (BearerTokenValidationFilter::extract)
                .filter (matchBasicLength)
                .flatMap (isolateBasicValue)
                .flatMap (basicToken -> {
                    String decodedToken = new String (Base64.getDecoder ().decode (basicToken));
                    String[] decodedCreds = decodedToken.split (":");
                    if (decodedCreds.length < 2) {
                        return Mono.error (new IllegalArgumentException ("Invalid credentials provided"));
                    }
                    String username = decodedCreds[0];
                    String password = decodedCreds[1];

                    if (!username.equals (clientSecret) || !password.equals (clientPassword)) {
                        return Mono.error (new IllegalArgumentException ("Invalid credentials"));
                    }

                    String internalToken = jwtUtilService.generateJwt ("INTEGRATOR", new ArrayList<> (),
                            List.of ("INTEGRATOR"));
                    exchange.getRequest ().getHeaders ().set (HttpHeaders.AUTHORIZATION, internalToken);
                    return chain.filter (exchange);
                })
                .onErrorResume (err -> {
                    ServerHttpResponse response = exchange.getResponse ();
                    DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory ()
                            .wrap (gson.toJson (UniversalResponse.builder ()
                                    .status (400).message ("Failed to validate session.").build ()).getBytes ());
                    response.setStatusCode (HttpStatus.UNAUTHORIZED);
                    response.getHeaders ().setContentType (MediaType.APPLICATION_JSON);
                    return exchange.getResponse ().writeWith (Mono.just (bodyDataBuffer))
                            .flatMap (exc -> exchange.getResponse ().setComplete ());
                });
    }
}
