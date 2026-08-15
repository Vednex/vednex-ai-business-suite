package com.vednex.gateway.web;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRequestWebFilter implements WebFilter {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
	private static final Logger log = LoggerFactory.getLogger(GatewayRequestWebFilter.class);
	private static final Pattern BEARER_TOKEN = Pattern.compile("^Bearer\\s+\\S+$");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		long startNanos = System.nanoTime();
		String correlationId = correlationId(exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER));
		ServerHttpRequest request = exchange.getRequest().mutate()
				.headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId))
				.build();
		ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
		mutatedExchange.getResponse().beforeCommit(() -> {
			mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
			return Mono.empty();
		});

		String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		Mono<Void> outcome;
		if (authorization != null && !authorization.isBlank() && !BEARER_TOKEN.matcher(authorization).matches()) {
			mutatedExchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			outcome = mutatedExchange.getResponse().setComplete();
		}
		else {
			outcome = chain.filter(mutatedExchange);
		}

		return outcome.doFinally(signalType -> logRequest(mutatedExchange, correlationId, startNanos, signalType));
	}

	private String correlationId(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return UUID.randomUUID().toString();
		}
		try {
			return UUID.fromString(candidate.trim()).toString();
		}
		catch (IllegalArgumentException ignored) {
			return UUID.randomUUID().toString();
		}
	}

	private void logRequest(ServerWebExchange exchange, String correlationId, long startNanos, SignalType signalType) {
		long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
		HttpStatusCode status = exchange.getResponse().getStatusCode();
		MDC.put("correlationId", correlationId);
		try {
			log.info("timestamp={} method={} path={} status={} durationMs={} correlationId={} signal={}",
					Instant.now(),
					exchange.getRequest().getMethod(),
					exchange.getRequest().getURI().getRawPath(),
					status == null ? null : status.value(),
					durationMillis,
					correlationId,
					signalType);
		}
		finally {
			MDC.remove("correlationId");
		}
	}

}
