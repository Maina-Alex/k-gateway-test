package com.ekenya.apigateway.filters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
    private final WebClient webClient;
    private final LoadBalancerClientFactory clientFactory;

    @Value ("${routes.AUTH-SERVER}")
    private String authServerRoute;

    private final Gson gson = new Gson ();



    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            exchange.getResponse ().setStatusCode (HttpStatus.UNAUTHORIZED);
            ServerHttpResponse servletResponse = exchange.getResponse ();
            if (!exchange.getRequest ().getHeaders ().containsKey ("Authorization")) {
                log.info ("Unauthorized request from " + exchange.getRequest ().getRemoteAddress () + " : Missing token headers");
                LinkedHashMap<String, Object> response = new LinkedHashMap<> ();
                response.put ("error", "Unauthorized");
                response.put ("error description", "Kindly Login");
                servletResponse.getHeaders ().add ("AuthStatus", "Unauthorized");
                Type gsonType = new TypeToken<LinkedHashMap<String, String>> () {
                }.getType ();
                String gsonString = gson.toJson (response, gsonType);
                DataBuffer bodyDataBuffer = servletResponse.bufferFactory ().wrap (gsonString.getBytes ());
                servletResponse.setStatusCode (HttpStatus.OK);
                servletResponse.writeWith (Mono.just (bodyDataBuffer));
                exchange.mutate ().response (servletResponse).build ();
                return exchange.getResponse ().setComplete ();
            } else {
                ReactiveLoadBalancer<ServiceInstance> jsonLoader = clientFactory.getInstance (authServerRoute);
                Mono<Response<ServiceInstance>> chosen = Mono.from (jsonLoader.choose ());
                Mono<String> serverUrlMono = chosen.map (serviceInstanceResponse -> {
//                    String url= "http://"+serviceInstanceResponse.getServer ().getHost ()+":"+serviceInstanceResponse.getServer ().getPort ();
                    String url= serviceInstanceResponse.getServer ().getUri ().toString () + "/oauth/token/check_token";

                    System.out.println (url);
                    return  url;
                });

                return serverUrlMono.flatMap (url -> {
                    String token = Objects.requireNonNull (exchange.getRequest ().getHeaders ().get ("Authorization")).get (0);
                    String tokenValue = token.substring (7);
                    Mono<ResponseEntity<Void>> tokenResponse = webClient
                            .post ()
                            .uri (url)
                            .bodyValue (Map.of ("token",tokenValue))
                            .retrieve ()
                            .toBodilessEntity ();
                    return tokenResponse.flatMap (res -> {
                        if (res.getStatusCodeValue () != 200) {
                            LinkedHashMap<String, Object> response = new LinkedHashMap<> ();
                            response.put ("error", "Unauthorized");
                            response.put ("error description", "Invalid Credentials");
                            String gsonString = gson.toJson (response);
                            DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory ().wrap (gsonString.getBytes ());
                            exchange.getResponse ().setStatusCode (HttpStatus.OK);
                            exchange.getResponse ().writeWith (Mono.just (bodyDataBuffer)).subscribe ();
                            return exchange.getResponse ().setComplete ();
                        } else {
                            return chain.filter (exchange);
                        }
                    });
                });
            }
        });
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Config {
        private boolean preLogger;
        private boolean postLogger;
    }

}
