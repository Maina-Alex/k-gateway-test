package com.ekenya.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author Alex Maina
 * @created 13/03/2023
 *
 * Removes sensitive Authorization header key from transit
 * Key could be an internal bearer key, thus why we are removing it from the response
 **/

@Configuration
@Order(6)
public class BearerResponseFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if(exchange.getResponse ().getHeaders ().containsKey ("Authorization")){
            ServerHttpResponse response= exchange.getResponse ();
             response.getHeaders ().remove ("Authorization");
             exchange.mutate ().response (response);
             return chain.filter (exchange);
        }
       return chain.filter (exchange);
    }
}
