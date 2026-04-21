package com.demobasic.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startMs = System.currentTimeMillis();
        String method = String.valueOf(exchange.getRequest().getMethod());
        String path = exchange.getRequest().getPath().value();
        return chain.filter(exchange).doFinally(signalType -> {
            int status = exchange.getResponse().getStatusCode() == null
                    ? -1
                    : exchange.getResponse().getStatusCode().value();
            long latency = System.currentTimeMillis() - startMs;
            log.info("{} {} -> {} ({} ms)", method, path, status, latency);
        });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
