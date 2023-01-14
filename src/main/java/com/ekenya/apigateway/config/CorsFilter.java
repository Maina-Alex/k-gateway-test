package com.ekenya.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Maina
 * @created 17/03/2022
 **/
@Configuration
public class CorsFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        MultiValueMap<String,String> headers= new LinkedMultiValueMap<>();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST,GET,OPTIONS");
        headers.add("Access-Control-Allow-Headers","*");
        headers.add("Access-Control-Expose-Headers","*");
        headers.add("Access-Control-Allow-Credentials" , "true");
        headers.add ("Strict-Transport-Security", "max-age=36500 ; includeSubDomains ; preload");
        headers.add("Content-Security-Policy","default-src 'self' https:; font-src 'self' https: data:; img-src 'self' https: data:; object-src 'none'; script-src https:; style-src 'self' https: 'unsafe-inline'");
        exchange.getResponse().getHeaders().addAll(headers);

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }
        return chain.filter(exchange);
    }
}
