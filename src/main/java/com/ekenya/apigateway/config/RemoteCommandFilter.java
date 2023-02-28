package com.ekenya.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 23/02/2023
 **/
public class RemoteCommandFilter implements GatewayFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest ();
        HttpHeaders headers = request.getHeaders ();

        for (String headerName : headers.keySet ()) {
            String headerValue = headers.getFirst (headerName);
            if ( headerValue!=null && containsShellCommand (headerValue)) {
                headers.remove (headerName);
            }
        }
        Consumer<HttpHeaders> headersConsumer = httpHeaders -> httpHeaders.addAll(headers);
        return chain.filter (exchange.mutate ().request (request.mutate ()
                .headers (headersConsumer).build ()).build ());
    }

    private boolean containsShellCommand(String value) {
        return value.contains ("&") || value.contains (";") || value.contains ("|") || value.contains ("`") || value.contains ("$(");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class RemoteCommandFilterFactory implements GatewayFilterFactory<Object> {

        @Override
        public GatewayFilter apply(Object config) {
            return new RemoteCommandFilter ();
        }

        @Override
        public String name() {
            return "RemoteCommand";
        }
    }
}

