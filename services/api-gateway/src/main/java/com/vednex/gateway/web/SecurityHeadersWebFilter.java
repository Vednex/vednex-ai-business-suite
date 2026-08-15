package com.vednex.gateway.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecurityHeadersWebFilter implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		exchange.getResponse().beforeCommit(() -> {
			HttpHeaders headers = exchange.getResponse().getHeaders();
			headers.set("X-Content-Type-Options", "nosniff");
			headers.set("X-Frame-Options", "DENY");
			headers.set("Referrer-Policy", "no-referrer");
			headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
			return Mono.empty();
		});
		return chain.filter(exchange);
	}

}
