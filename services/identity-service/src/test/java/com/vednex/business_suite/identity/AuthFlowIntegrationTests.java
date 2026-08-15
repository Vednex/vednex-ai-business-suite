package com.vednex.business_suite.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.repository.CompanyRepository;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.domain.UserStatus;
import com.vednex.business_suite.identity.repository.EmailVerificationTokenRepository;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.security.TestRsaKeys;
import com.vednex.business_suite.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"app.frontend-url=http://localhost:3000",
		"app.outbox.enabled=false"
})
@Sql(statements = {
		"delete from audit_logs",
		"delete from outbox_events",
		"delete from refresh_tokens",
		"delete from password_reset_tokens",
		"delete from email_verification_tokens",
		"delete from user_roles",
		"delete from company_members",
		"delete from invitations",
		"delete from subscriptions",
		"delete from company_settings",
		"delete from users",
		"delete from companies"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthFlowIntegrationTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Autowired
	TestRestTemplate restTemplate;

	@LocalServerPort
	int port;

	@Autowired
	UserRepository userRepository;

	@Autowired
	CompanyRepository companyRepository;

	@Autowired
	CompanyMemberRepository companyMemberRepository;

	@Autowired
	EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	TokenService tokenService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("app.jwt.private-key", TestRsaKeys::privateKey);
		registry.add("app.jwt.public-key", TestRsaKeys::publicKey);
	}

	@BeforeEach
	void resetOutbox() {
	}

	@Test
	void registrationCreatesPendingUserAndCompanyThenQueuesEmailAfterCommit() {
		ResponseEntity<Map> response = register("new@example.com", "New Company");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).doesNotContainKeys("accessToken", "refreshToken");
		assertThat(response.toString()).doesNotContain("/verify-email?token=");

		User user = userRepository.findByEmailIgnoreCase("new@example.com").orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
		assertThat(user.getEmailVerified()).isFalse();
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyMemberRepository.count()).isEqualTo(1);
		assertThat(companyMemberRepository.findFirstByUserIdAndMembershipStatusAndActiveTrue(user.getId(), MembershipStatus.ACTIVE)).isPresent();
		assertThat(emailVerificationTokenRepository.count()).isEqualTo(1);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(1);
	}

	@Test
	void duplicatePendingRegistrationIsCaseInsensitiveAndDoesNotCreateDuplicateRecords() {
		register("Prit@Example.com", "Pending Company");

		ResponseEntity<Map> duplicate = register("prit@example.com", "Second Company");

		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(errorCode(duplicate)).isEqualTo("EMAIL_VERIFICATION_PENDING");
		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyMemberRepository.count()).isEqualTo(1);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(1);
	}

	@Test
	void duplicateActiveVerifiedRegistrationReturnsAccountAlreadyExists() {
		register("active@example.com", "Active Company");
		verify(latestVerificationToken());
		long emailEventsBeforeDuplicate = outboxEventCount("identity.email.verification.requested.v1");

		ResponseEntity<Map> duplicate = register("ACTIVE@example.com", "Duplicate Active Company");

		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(errorCode(duplicate)).isEqualTo("ACCOUNT_ALREADY_EXISTS");
		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyMemberRepository.count()).isEqualTo(1);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(emailEventsBeforeDuplicate);
	}

	@Test
	void resendCreatesNewTokenInvalidatesOldTokenAndKeepsNewTokenOneTimeUse() {
		register("resend@example.com", "Resend Company");
		String oldToken = latestVerificationToken();
		moveVerificationCooldownBack("resend@example.com");
		long emailEventsBeforeResend = outboxEventCount("identity.email.verification.requested.v1");

		ResponseEntity<Map> resend = resend("resend@example.com");
		String newToken = latestVerificationToken();

		assertThat(resend.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(emailVerificationTokenRepository.count()).isEqualTo(2);
		assertThat(emailVerificationTokenRepository.findByTokenHash(tokenService.hashToken(oldToken)).orElseThrow().getUsedAt()).isNotNull();
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(emailEventsBeforeResend + 1);
		assertThat(verify(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(verify(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(verify(newToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void resendCooldownStillAppliesAndVerifiedUserDoesNotReceiveNewToken() {
		register("cooldown@example.com", "Cooldown Company");

		ResponseEntity<Map> cooldown = resend("cooldown@example.com");

		assertThat(cooldown.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(errorCode(cooldown)).isEqualTo("VERIFICATION_RESEND_COOLDOWN");

		String token = latestVerificationToken();
		assertThat(verify(token).getStatusCode()).isEqualTo(HttpStatus.OK);
		long tokenCount = emailVerificationTokenRepository.count();
		long emailEventsBeforeVerifiedResend = outboxEventCount("identity.email.verification.requested.v1");

		ResponseEntity<Map> verifiedResend = resend("cooldown@example.com");

		assertThat(verifiedResend.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(emailVerificationTokenRepository.count()).isEqualTo(tokenCount);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(emailEventsBeforeVerifiedResend);
	}

	@Test
	void asyncEmailEventDoesNotBlockRegistrationAndUserCanResend() {
		ResponseEntity<Map> response = register("mailfail@example.com", "Mail Failure Company");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		User user = userRepository.findByEmailIgnoreCase("mailfail@example.com").orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
		assertThat(user.getEmailVerified()).isFalse();
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(1);

		moveVerificationCooldownBack("mailfail@example.com");

		ResponseEntity<Map> resend = resend("mailfail@example.com");

		assertThat(resend.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(2);
	}

	@Test
	void registrationRollbackDoesNotDeliverVerificationEmail() {
		register("rollback@example.com", "Rollback Company");
		long emailEventsBeforeDuplicate = outboxEventCount("identity.email.verification.requested.v1");

		ResponseEntity<Map> duplicate = register("rollback@example.com", "Rollback Duplicate");

		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(outboxEventCount("identity.email.verification.requested.v1")).isEqualTo(emailEventsBeforeDuplicate);
	}

	private ResponseEntity<Map> register(String email, String companyName) {
		return restTemplate.postForEntity("/api/auth/register", Map.of(
				"companyName", companyName,
				"firstName", "Test",
				"lastName", "User",
				"email", email,
				"password", "Password1",
				"country", "India",
				"timezone", "Asia/Kolkata"
		), Map.class);
	}

	private ResponseEntity<Map> resend(String email) {
		return restTemplate.postForEntity("/api/auth/resend-verification", Map.of("email", email), Map.class);
	}

	private ResponseEntity<Map> verify(String token) {
		String body = "{\"token\":\"" + token + "\"}";
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/verify-email"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		try {
			HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return ResponseEntity.status(response.statusCode()).body(Map.of());
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to call verify-email endpoint", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while calling verify-email endpoint", ex);
		}
	}

	private String latestVerificationToken() {
		String url = jdbcTemplate.queryForObject("""
				select payload->'data'->>'verificationUrl'
				from outbox_events
				where event_type = 'identity.email.verification.requested.v1'
				order by created_at desc
				limit 1
				""", String.class);
		assertThat(url).isNotBlank();
		return url.substring(url.indexOf("token=") + 6);
	}

	private long outboxEventCount(String eventType) {
		Long count = jdbcTemplate.queryForObject("select count(*) from outbox_events where event_type = ?", Long.class, eventType);
		return count == null ? 0 : count;
	}

	private String errorCode(ResponseEntity<Map> response) {
		return (String) response.getBody().get("code");
	}

	private void moveVerificationCooldownBack(String email) {
		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		jdbcTemplate.update("""
				update email_verification_tokens
				set last_sent_at = current_timestamp - interval '10 minutes'
				where user_id = ?
				""", user.getId());
	}

}
