package com.vednex.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtGatewayAuthenticationWebFilter implements WebFilter {

	private static final Pattern BEARER_TOKEN = Pattern.compile("^Bearer\\s+\\S+$");

	private static final List<String> PROTECTED_PREFIXES = List.of(
			"/api/company",
			"/api/users",
			"/api/roles",
			"/api/permissions",
			"/api/subscriptions",
			"/api/crm"
	);

	private static final List<String> PUBLIC_EXACT_PATHS = List.of(
			"/api/auth/register",
			"/api/auth/verify-email",
			"/api/auth/resend-verification",
			"/api/auth/login",
			"/api/auth/refresh",
			"/api/auth/logout",
			"/api/auth/forgot-password",
			"/api/auth/reset-password",
			"/swagger-ui.html"
	);

	private static final List<String> PUBLIC_PREFIXES = List.of(
			"/actuator/health",
			"/api/public",
			"/api/company/invitations/",
			"/v3/api-docs",
			"/swagger-ui"
	);

	private final JwtAccessTokenValidator tokenValidator;
	private final List<String> allowedOrigins;

	public JwtGatewayAuthenticationWebFilter(
			JwtAccessTokenValidator tokenValidator,
			@Value("${gateway.cors.allowed-origins}") String allowedOrigins
	) {
		this.tokenValidator = tokenValidator;
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (!requiresAccessToken(exchange)) {
			return chain.filter(exchange);
		}

		String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !BEARER_TOKEN.matcher(authorization).matches()) {
			return unauthorized(exchange);
		}

		try {
			tokenValidator.validateAccessToken(authorization.substring(7).trim());
		}
		catch (JwtAccessTokenValidator.JwtValidationException ex) {
			return unauthorized(exchange);
		}
		return chain.filter(exchange);
	}

	private boolean requiresAccessToken(ServerWebExchange exchange) {
		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
			return false;
		}
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if (PUBLIC_EXACT_PATHS.contains(path) || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
			return false;
		}
		if (path.equals("/api/auth/switch-company")) {
			return true;
		}
		return PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		addCorsHeaders(exchange);
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		String body = """
				{"success":false,"code":"UNAUTHENTICATED","message":"Authentication is required","fieldErrors":[],"timestamp":"%s","path":"%s"}"""
				.formatted(Instant.now(), path);
		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}

	private void addCorsHeaders(ServerWebExchange exchange) {
		String origin = exchange.getRequest().getHeaders().getOrigin();
		if (origin == null || !allowedOrigins.contains(origin)) {
			return;
		}
		HttpHeaders headers = exchange.getResponse().getHeaders();
		headers.setAccessControlAllowOrigin(origin);
		headers.setAccessControlAllowCredentials(true);
		headers.setAccessControlExposeHeaders(List.of("X-Correlation-ID", "X-Correlation-Id"));
	}

}
