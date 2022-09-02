package com.ekenya.apigateway.filters;


import com.ekenya.apigateway.model.UniversalResponse;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class WorkflowFilter extends AbstractGatewayFilterFactory<WorkflowFilter.Config> {
    @Autowired
    private  WebClient webClient;
    @Autowired
    private  ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory;

    @Value ("${routes.WORKFLOW}")
    private String workflowRoute;

    private final Gson gson = new Gson ();

    public WorkflowFilter() {
        super (WorkflowFilter.Config.class);
    }

    @Override
    public GatewayFilter apply(WorkflowFilter.Config config) {
        return ((exchange, chain) -> {
            ReactiveLoadBalancer<ServiceInstance> jsonLoader = serviceInstanceFactory.getInstance (workflowRoute);
            Mono<Response<ServiceInstance>> chosen = Mono.from (jsonLoader.choose ());
            Mono<String> serverUrlMono = chosen.map (serviceInstanceResponse -> {
                String url= "http://"+serviceInstanceResponse.getServer ().getHost ()+":"+serviceInstanceResponse.getServer ().getPort ();
                return url+ "/api/v1/workflow/process";
            });


            return serverUrlMono.flatMap (url -> {
                Flux<DataBuffer> dataBufferFlux = exchange.getRequest ().getBody ();
                Mono<String> body = decodeToMono (dataBufferFlux);
                return body.flatMap (b -> {
                    Mono<UniversalResponse> universalResponseMono = webClient.post ()
                            .uri (URI.create (url))
                            .bodyValue (b)
                            .retrieve ()
                            .bodyToMono (UniversalResponse.class);

                    return universalResponseMono.flatMap (res -> {
                        if (res.getStatus () == 200) {
                            return chain.filter (exchange);
                        }else{
                            String gsonString = gson.toJson(res);
                            DataBuffer bodyDataBuffer = exchange.getResponse ().bufferFactory().wrap(gsonString.getBytes());
                            exchange.getResponse ().setStatusCode(HttpStatus.OK);
                            exchange.getResponse ().writeWith(Mono.just(bodyDataBuffer)).subscribe();
                            exchange.mutate().response(exchange.getResponse ()).build();
                            return exchange.getResponse ().setComplete ();
                        }
                    });
                });
            });
    });
}

    private String decodeDataBuffer(DataBuffer dataBuffer) {
        Charset charset = StandardCharsets.UTF_8;
        CharBuffer charBuffer = charset.decode(dataBuffer.asByteBuffer());
        DataBufferUtils.release(dataBuffer);
        return charBuffer.toString();
    }

    public Mono<String> decodeToMono(Publisher<DataBuffer> inputStream) {
        return Flux.from(inputStream)
                .reduce(DataBuffer::write)
                .map(this::decodeDataBuffer);
    }


@Data
@AllArgsConstructor
@NoArgsConstructor
public static class Config {
    private boolean preLogger;
    private boolean postLogger;
}
}
