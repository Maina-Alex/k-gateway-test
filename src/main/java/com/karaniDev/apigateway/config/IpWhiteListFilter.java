package com.karaniDev.apigateway.config;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alex Maina
 * @created 15/09/2022
 **/
@Configuration
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class IpWhiteListFilter  implements GlobalFilter {
    @Value ("${IP.WHITELIST}")
    private List<String> ipWhiteListRange;
    @Value ("${IP.WHITELIST-ON}")
    private boolean whiteListOn;
    private final Gson gson;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String IP_RANGE= exchange.getRequest ().getURI ().getScheme ()+"://"+exchange.getRequest ().getURI ().getHost ();
        log.info ("Accessing resource {}", exchange.getRequest ().getURI ().getPath ());
        log.info ("IP RANGE {}", IP_RANGE);
        if(whiteListOn && !ipWhiteListRange.contains (IP_RANGE)){
            log.info ("Un-whitelisted ip address {}",IP_RANGE);
            Map<String,Object> response= new HashMap<> ();
            response.put ("status",400);
            response.put ("message","Unauthorized Host Access");
            String gsonString = gson.toJson (response);
            DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory ().wrap (gsonString.getBytes ());
            exchange.getResponse ().setStatusCode (HttpStatus.OK);
            return exchange.getResponse ().writeWith (Mono.just (bodyDataBuffer))
                    .flatMap (res-> exchange.getResponse ().setComplete ());
        }
        return chain.filter(exchange);
    }

}
