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
import org.springframework.http.server.reactive.ServerHttpRequest;
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
@Slf4j
public class BasicAuthenticationFilter implements WebFilter {
    @Value("${services.integration.basic.client}")
    private String clientSecret;
    @Value("${services.integration.basic.password}")
    private String clientPassword;
    private final Gson gson;
    private final JwtUtilService jwtUtilService;
    private static final String BASIC = "Basic ";
    private static final String INTERNAL_TOKEN_HEADER ="INTERNAL_TOKEN";
    private static final String BEARER = "Bearer ";
    //prevents matching Auth server channel key header
    private static final Predicate<String> matchBasicLength = authValue -> {
        String [] bearerSplit= authValue.split (" ");
        if(bearerSplit.length<2) return false;
        return bearerSplit[0].length ()== BASIC.trim ().length ();
    };
    private static final Function<String, Mono<String>> isolateBasicValue = authValue -> Mono.justOrEmpty (authValue.substring (BASIC.length ()));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if(exchange.getRequest ().getURI ().getPath ().contains ("oauth")){
            return chain.filter (exchange);
        }
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
                    HttpHeaders newHeaders= new HttpHeaders ();
                    newHeaders.add (INTERNAL_TOKEN_HEADER, BEARER +internalToken);
                    ServerHttpRequest newRequest = exchange.getRequest().mutate().headers(h -> h.addAll(newHeaders)).build();
                    return chain.filter(exchange.mutate().request(newRequest).build());
                })
                .onErrorResume (err -> {
                    log.error ("An error occurred on basic auth filter",err);
                    ServerHttpResponse response = exchange.getResponse ();
                    DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory ()
                            .wrap (gson.toJson (UniversalResponse.builder ()
                                    .status (400).message (err.getMessage ()).build ()).getBytes ());
                    response.getHeaders ().setContentType (MediaType.APPLICATION_JSON);
                    response.setStatusCode (HttpStatus.UNAUTHORIZED);
                    return response.writeWith (Mono.just (bodyDataBuffer))
                            .flatMap (exc -> response.setComplete ());
                })
                .switchIfEmpty (chain.filter (exchange));
    }
}
