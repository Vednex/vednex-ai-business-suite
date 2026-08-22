package com.vednex.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

	private static final DownstreamServer downstream = DownstreamServer.start();

	@LocalServerPort
	int port;

	@Autowired
	WebTestClient webTestClient;

	@DynamicPropertySource
	static void gatewayProperties(DynamicPropertyRegistry registry) {
		registry.add("IDENTITY_SERVICE_URL", downstream::baseUrl);
		registry.add("CRM_SERVICE_URL", downstream::baseUrl);
		registry.add("FRONTEND_ALLOWED_ORIGINS",
				() -> "http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001");
		registry.add("JWT_PUBLIC_KEY", TestJwtTokens::publicKey);
	}

	@AfterAll
	static void stopDownstream() {
		downstream.stop();
	}

	@Test
	void authLoginRoutesToIdentityService() {
		assertRoute("POST", "/api/auth/login");
	}

	@Test
	void authRegisterRoutesToIdentityService() {
		assertRoute("POST", "/api/auth/register");
	}

	@Test
	void companyRoutesToIdentityService() {
		assertProtectedRoute("GET", "/api/company");
	}

	@Test
	void usersRoutesToIdentityService() {
		assertProtectedRoute("GET", "/api/users/me");
	}

	@Test
	void rolesRoutesToIdentityService() {
		assertProtectedRoute("GET", "/api/roles");
	}

	@Test
	void permissionsRoutesToIdentityService() {
		assertProtectedRoute("GET", "/api/permissions");
	}

	@Test
	void subscriptionsRoutesToIdentityService() {
		assertProtectedRoute("GET", "/api/subscriptions");
	}

	@Test
	void crmRoutesToCrmService() {
		assertProtectedRoute("GET", "/api/crm/contacts");
	}

	@Test
	void switchCompanyRequiresValidJwtAndRoutesToIdentityService() {
		assertProtectedRoute("POST", "/api/auth/switch-company");
	}

	@Test
	void correlationIdIsGeneratedWhenMissing() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.validToken())
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value("X-Correlation-ID", value -> assertThat(UUID.fromString(value)).isNotNull());
	}

	@Test
	void correlationIdIsPreservedWhenProvided() {
		String correlationId = UUID.randomUUID().toString();
		webTestClient.get()
				.uri("/api/company")
				.header("X-Correlation-ID", correlationId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.validToken())
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("X-Correlation-ID", correlationId)
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("\"correlationId\":\"" + correlationId + "\""));
	}

	@Test
	void corsPreflightAllowsFrontendOrigin() {
		webTestClient.options()
				.uri("/api/auth/login")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
	}

	@Test
	void corsPreflightAllowsFallbackFrontendDevOrigin() {
		webTestClient.options()
				.uri("/api/auth/register")
				.header(HttpHeaders.ORIGIN, "http://localhost:3001")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3001")
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
	}

	@Test
	void healthEndpointReportsGatewayHealth() {
		webTestClient.get()
				.uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("\"status\":\"UP\""));
	}

	@Test
	void unknownRouteReturnsNotFound() {
		webTestClient.get()
				.uri("/not-routed")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void malformedAuthorizationHeaderIsRejectedBeforeForwarding() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Basic abc")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void publicRefreshRouteWorksWithoutAccessJwt() {
		assertRoute("POST", "/api/auth/refresh");
	}

	@Test
	void protectedRouteWithoutJwtReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/company")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void crmRouteWithoutJwtReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/crm/contacts")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void protectedRouteWithMalformedJwtReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void protectedRouteWithExpiredJwtReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.expiredToken())
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void protectedRouteWithWrongSignatureReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.wrongSignatureToken())
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void protectedRouteWithRefreshTypeJwtReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.refreshTypeToken())
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void authorizationHeaderIsForwardedDownstreamAfterValidation() {
		webTestClient.get()
				.uri("/api/company")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.validToken())
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("\"authorizationPresent\":true"));
	}

	private void assertRoute(String method, String path) {
		WebTestClient.RequestBodySpec request = webTestClient.method(org.springframework.http.HttpMethod.valueOf(method))
				.uri(path)
				.header("Content-Type", "application/json");
		if ("POST".equals(method)) {
			request.bodyValue("{}")
					.exchange()
					.expectStatus().isOk()
					.expectBody(String.class)
					.value(body -> assertThat(body).contains("\"method\":\"" + method + "\"", "\"path\":\"" + path + "\""));
		}
		else {
			request.exchange()
					.expectStatus().isOk()
					.expectBody(String.class)
					.value(body -> assertThat(body).contains("\"method\":\"" + method + "\"", "\"path\":\"" + path + "\""));
		}
	}

	private void assertProtectedRoute(String method, String path) {
		WebTestClient.RequestBodySpec request = webTestClient.method(org.springframework.http.HttpMethod.valueOf(method))
				.uri(path)
				.header("Content-Type", "application/json")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.validToken());
		request.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("\"method\":\"" + method + "\"", "\"path\":\"" + path + "\""));
	}

	private static final class DownstreamServer {

		private final HttpServer server;

		private DownstreamServer(HttpServer server) {
			this.server = server;
		}

		static DownstreamServer start() {
			try {
				HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
				server.createContext("/", DownstreamServer::handle);
				server.setExecutor(Executors.newCachedThreadPool());
				server.start();
				return new DownstreamServer(server);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not start downstream test server", ex);
			}
		}

		String baseUrl() {
			return "http://localhost:" + server.getAddress().getPort();
		}

		void stop() {
			server.stop(0);
		}

		private static void handle(HttpExchange exchange) throws IOException {
			String body = """
					{"method":"%s","path":"%s","correlationId":"%s"}""".formatted(
					exchange.getRequestMethod(),
					exchange.getRequestURI().getRawPath(),
					exchange.getRequestHeaders().getFirst("X-Correlation-ID"))
					.replace("}", ",\"authorizationPresent\":" + (exchange.getRequestHeaders().getFirst("Authorization") != null) + "}");
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(bytes);
			}
		}

	}

}
