package com.vednex.business_suite.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vednex.business_suite.company.domain.CompanyMember;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.repository.EmailVerificationTokenRepository;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.security.TestRsaKeys;
import com.vednex.business_suite.security.TokenService;
import com.vednex.business_suite.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;
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
class CompanyUserListingIntegrationTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	UserRepository userRepository;

	@Autowired
	CompanyMemberRepository companyMemberRepository;

	@Autowired
	EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@Autowired
	TokenService tokenService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	TransactionTemplate transactionTemplate;

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
	void companyUserListReturnsCurrentCompanyUsersWithOwnerRole() {
		AuthSession owner = verifiedLogin("owner@example.com", "Owner Company");

		ResponseEntity<Map> response = listUsers(owner.accessToken(), "");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> users = content(response);
		assertThat(users).extracting(user -> user.get("email")).contains("owner@example.com");
		Map<String, Object> row = rowByEmail(response, "owner@example.com");
		assertThat(row.get("membershipStatus")).isEqualTo("ACTIVE");
		assertThat(row.get("userStatus")).isEqualTo("ACTIVE");
		assertThat((List<String>) row.get("roles")).contains("OWNER");
	}

	@Test
	void acceptedInvitationUserAppearsInCompanyUsersList() {
		AuthSession owner = verifiedLogin("invite-owner@example.com", "Invite Owner Company");
		invite(owner.accessToken(), "accepted-user@example.com", ownerRoleId());

		acceptInvitation(latestInvitationToken(), "Accepted", "User");
		ResponseEntity<Map> response = listUsers(owner.accessToken(), "");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(content(response)).extracting(user -> user.get("email")).contains("accepted-user@example.com");
	}

	@Test
	void companyCannotSeeAnotherCompanyUsers() {
		AuthSession companyA = verifiedLogin("company-a@example.com", "Company A");
		verifiedLogin("company-b@example.com", "Company B");

		ResponseEntity<Map> response = listUsers(companyA.accessToken(), "");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(content(response)).extracting(user -> user.get("email"))
				.contains("company-a@example.com")
				.doesNotContain("company-b@example.com");
	}

	@Test
	void searchMatchesFirstNameLastNameEmailAndIsCaseInsensitive() {
		AuthSession owner = verifiedLogin("search-owner@example.com", "Search Owner Company");
		invite(owner.accessToken(), "prit.gol@example.com", ownerRoleId());
		acceptInvitation(latestInvitationToken(), "Prit", "Gol");

		assertThat(content(listUsers(owner.accessToken(), "prit"))).extracting(user -> user.get("email")).contains("prit.gol@example.com");
		assertThat(content(listUsers(owner.accessToken(), "gol"))).extracting(user -> user.get("email")).contains("prit.gol@example.com");
		assertThat(content(listUsers(owner.accessToken(), "PRIT.GOL@EXAMPLE.COM"))).extracting(user -> user.get("email")).contains("prit.gol@example.com");
		assertThat(content(listUsers(owner.accessToken(), "Prit Gol"))).extracting(user -> user.get("email")).contains("prit.gol@example.com");
	}

	@Test
	void paginationReturnsRequestedPageAndTotals() {
		AuthSession owner = verifiedLogin("page-owner@example.com", "Page Owner Company");
		for (int i = 0; i < 3; i++) {
			invite(owner.accessToken(), "page-user-" + i + "@example.com", ownerRoleId());
			acceptInvitation(latestInvitationToken(), "Page", "User" + i);
		}

		ResponseEntity<Map> response = listUsers(owner.accessToken(), "?page=0&size=2");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(content(response)).hasSize(2);
		assertThat(data(response).get("number")).isEqualTo(0);
		assertThat(data(response).get("size")).isEqualTo(2);
		assertThat(((Number) data(response).get("totalElements")).longValue()).isEqualTo(4);
	}

	@Test
	void invalidPaginationParametersReturnBadRequest() {
		AuthSession owner = verifiedLogin("invalid-page-owner@example.com", "Invalid Page Owner Company");

		ResponseEntity<Map> invalidPage = listUsers(owner.accessToken(), "?page=-1&size=10");
		ResponseEntity<Map> invalidSize = listUsers(owner.accessToken(), "?page=0&size=0");

		assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(invalidSize.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void membershipStatusAndAssignedRolesAreReturned() {
		AuthSession owner = verifiedLogin("status-owner@example.com", "Status Owner Company");
		CompanyMember member = membership(owner.userId());
		member.setMembershipStatus(MembershipStatus.SUSPENDED);
		companyMemberRepository.saveAndFlush(member);

		ResponseEntity<Map> response = listUsers(owner.accessToken(), "?status=SUSPENDED");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> row = rowByEmail(response, "status-owner@example.com");
		assertThat(row.get("membershipStatus")).isEqualTo("SUSPENDED");
		assertThat((List<String>) row.get("roles")).contains("OWNER");
	}

	@Test
	void userWithoutUserViewGetsForbiddenAndUnauthenticatedGetsUnauthorized() {
		AuthSession owner = verifiedLogin("limited-owner@example.com", "Limited Owner Company");
		replaceWithRoleWithoutUserView(owner);
		ResponseEntity<Map> limitedLogin = login("limited-owner@example.com");

		ResponseEntity<Map> forbidden = listUsers(authValue(limitedLogin, "accessToken"), "");
		ResponseEntity<Map> unauthenticated = restTemplate.getForEntity("/api/company/users", Map.class);

		assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void removedMembershipIsExcludedByDefaultButCanBeRequestedExplicitly() {
		AuthSession owner = verifiedLogin("removed-owner@example.com", "Removed Owner Company");
		CompanyMember member = membership(owner.userId());
		member.setMembershipStatus(MembershipStatus.REMOVED);
		companyMemberRepository.saveAndFlush(member);

		ResponseEntity<Map> defaultResponse = listUsers(owner.accessToken(), "");
		ResponseEntity<Map> removedResponse = listUsers(owner.accessToken(), "?status=REMOVED");

		assertThat(content(defaultResponse)).extracting(user -> user.get("email")).doesNotContain("removed-owner@example.com");
		assertThat(content(removedResponse)).extracting(user -> user.get("email")).contains("removed-owner@example.com");
	}

	@Test
	void companyUsersResponseDoesNotExposeSensitiveFields() {
		AuthSession owner = verifiedLogin("safe-owner@example.com", "Safe Owner Company");

		ResponseEntity<Map> response = listUsers(owner.accessToken(), "");

		assertThat(response.toString()).doesNotContain("passwordHash", "refreshToken", "verification", "reset", "failedLoginAttempts");
	}

	private AuthSession verifiedLogin(String email, String companyName) {
		register(email, companyName);
		verify(latestVerificationToken());
		ResponseEntity<Map> login = login(email);
		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		return new AuthSession(user.getId(), authValue(login, "accessToken"));
	}

	private ResponseEntity<Map> register(String email, String companyName) {
		return restTemplate.postForEntity("/api/auth/register", Map.of(
				"companyName", companyName,
				"firstName", "Test",
				"lastName", "Owner",
				"email", email,
				"password", "Password1",
				"country", "India",
				"timezone", "Asia/Kolkata"
		), Map.class);
	}

	private ResponseEntity<Map> verify(String token) {
		return restTemplate.postForEntity("/api/auth/verify-email", Map.of("token", token), Map.class);
	}

	private ResponseEntity<Map> login(String email) {
		return restTemplate.postForEntity("/api/auth/login", Map.of(
				"email", email,
				"password", "Password1"
		), Map.class);
	}

	private void invite(String accessToken, String email, UUID roleId) {
		restTemplate.exchange("/api/company/invitations", HttpMethod.POST,
				new HttpEntity<>(Map.of("email", email, "roleId", roleId), headers(accessToken)), Map.class);
	}

	private void acceptInvitation(String token, String firstName, String lastName) {
		restTemplate.postForEntity("/api/company/invitations/" + token + "/accept", Map.of(
				"firstName", firstName,
				"lastName", lastName,
				"password", "Password1"
		), Map.class);
	}

	private ResponseEntity<Map> listUsers(String accessToken, String query) {
		String requestQuery = query.isBlank() ? "?page=0&size=10" : query;
		if (!requestQuery.startsWith("?")) {
			requestQuery = "?search=" + requestQuery + "&page=0&size=10";
		}
		return restTemplate.exchange("/api/company/users" + requestQuery, HttpMethod.GET,
				new HttpEntity<>(headers(accessToken)), Map.class);
	}

	private HttpHeaders headers(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		return headers;
	}

	private UUID ownerRoleId() {
		return jdbcTemplate.queryForObject("select id from roles where company_id is null and name = 'OWNER'", UUID.class);
	}

	private void replaceWithRoleWithoutUserView(AuthSession session) {
		UUID roleId = jdbcTemplate.queryForObject("""
				insert into roles (company_id, name, description, protected_system_role, active)
				values (null, ?, 'Limited test role', false, true)
				returning id
				""", UUID.class, "LIMITED_" + UUID.randomUUID());
		jdbcTemplate.update("""
				insert into role_permissions (role_id, permission_id)
				select ?, id from permissions where code = 'AI_USE'
				""", roleId);
		CompanyMember member = membership(session.userId());
		jdbcTemplate.update("delete from user_roles where membership_id = ?", member.getId());
		jdbcTemplate.update("""
				insert into user_roles (company_id, membership_id, user_id, role_id, active)
				values (?, ?, ?, ?, true)
				""", member.getCompanyId(), member.getId(), session.userId(), roleId);
		transactionTemplate.executeWithoutResult(status ->
				refreshTokenRepository.revokeActiveUserTokens(session.userId(), java.time.Instant.now()));
	}

	private CompanyMember membership(UUID userId) {
		return companyMemberRepository.findFirstByUserIdAndMembershipStatusAndActiveTrue(userId, MembershipStatus.ACTIVE)
				.orElseGet(() -> companyMemberRepository.findByUserIdAndActiveTrue(userId).get(0));
	}

	private String latestVerificationToken() {
		String url = latestEventUrl("identity.email.verification.requested.v1", "verificationUrl");
		return url.substring(url.indexOf("token=") + 6);
	}

	private String latestInvitationToken() {
		String url = latestEventUrl("identity.user.invited.v1", "invitationUrl");
		return url.substring(url.indexOf("token=") + 6);
	}

	private String latestEventUrl(String eventType, String fieldName) {
		String url = jdbcTemplate.queryForObject("""
				select payload->'data'->>?
				from outbox_events
				where event_type = ?
				order by created_at desc
				limit 1
				""", String.class, fieldName, eventType);
		assertThat(url).isNotBlank();
		return url;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> data(ResponseEntity<Map> response) {
		return (Map<String, Object>) response.getBody().get("data");
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> content(ResponseEntity<Map> response) {
		return (List<Map<String, Object>>) data(response).get("content");
	}

	private Map<String, Object> rowByEmail(ResponseEntity<Map> response, String email) {
		return content(response).stream()
				.filter(row -> email.equals(row.get("email")))
				.findFirst()
				.orElseThrow();
	}

	private String authValue(ResponseEntity<Map> response, String key) {
		return (String) ((Map<?, ?>) response.getBody().get("data")).get(key);
	}

	record AuthSession(UUID userId, String accessToken) {
	}

}
