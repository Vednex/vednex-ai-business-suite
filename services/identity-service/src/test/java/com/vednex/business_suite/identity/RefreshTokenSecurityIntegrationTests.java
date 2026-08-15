package com.vednex.business_suite.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vednex.business_suite.company.domain.Company;
import com.vednex.business_suite.company.domain.CompanyMember;
import com.vednex.business_suite.company.domain.CompanyStatus;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.repository.CompanyRepository;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.domain.UserStatus;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.security.JwtPrincipal;
import com.vednex.business_suite.security.JwtService;
import com.vednex.business_suite.security.TestRsaKeys;
import com.vednex.business_suite.security.TokenService;
import com.vednex.business_suite.security.domain.RefreshToken;
import com.vednex.business_suite.security.repository.RefreshTokenRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class RefreshTokenSecurityIntegrationTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Autowired
	TestRestTemplate restTemplate;

	@LocalServerPort
	int port;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	CompanyRepository companyRepository;

	@Autowired
	CompanyMemberRepository companyMemberRepository;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@Autowired
	TokenService tokenService;

	@Autowired
	JwtService jwtService;

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

	@Test
	void activeVerifiedUserWithActiveMembershipCanRefresh() {
		AuthSession session = verifiedLogin("active-refresh@example.com", "Active Refresh Co");

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(authValue(refresh, "accessToken")).isNotNull();
		assertThat(authValue(refresh, "refreshToken")).isNotEqualTo(session.refreshToken());
	}

	@Test
	void refreshRotatesTokenAndStoresReplacementInSameFamily() {
		AuthSession session = verifiedLogin("rotate@example.com", "Rotate Co");
		RefreshToken oldToken = tokenRecord(session.refreshToken());

		ResponseEntity<Map> refresh = refresh(session.refreshToken());
		String rotatedRawToken = authValue(refresh, "refreshToken");
		RefreshToken rotatedToken = tokenRecord(rotatedRawToken);
		RefreshToken reloadedOldToken = refreshTokenRepository.findById(oldToken.getId()).orElseThrow();

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(reloadedOldToken.getActive()).isFalse();
		assertThat(reloadedOldToken.getRevokedAt()).isNotNull();
		assertThat(reloadedOldToken.getReplacedByTokenId()).isEqualTo(rotatedToken.getId());
		assertThat(rotatedToken.getFamilyId()).isEqualTo(oldToken.getFamilyId());
		assertThat(rotatedToken.getActive()).isTrue();
	}

	@Test
	void oldRotatedTokenReuseIsRejectedAndRevokesFamily() {
		AuthSession session = verifiedLogin("reuse@example.com", "Reuse Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		ResponseEntity<Map> firstRefresh = refresh(session.refreshToken());

		ResponseEntity<Map> reuse = refresh(session.refreshToken());

		assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertFamilyRevoked(familyId);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from audit_logs where action = 'REFRESH_TOKEN_REUSED'",
				Long.class
		)).isEqualTo(1);
	}

	@Test
	void expiredRefreshTokenIsRejectedWithoutCreatingNewToken() {
		AuthSession session = verifiedLogin("expired@example.com", "Expired Co");
		RefreshToken token = tokenRecord(session.refreshToken());
		token.setExpiresAt(Instant.now().minusSeconds(60));
		refreshTokenRepository.saveAndFlush(token);
		long tokenCount = refreshTokenRepository.count();

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(refreshTokenRepository.count()).isEqualTo(tokenCount);
	}

	@Test
	void unknownRefreshTokenIsRejected() {
		ResponseEntity<Map> refresh = refresh(tokenService.createOpaqueToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("INVALID_TOKEN");
	}

	@Test
	void lockedUserStatusCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("locked-status@example.com", "Locked Status Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		User user = userRepository.findById(session.userId()).orElseThrow();
		user.setStatus(UserStatus.LOCKED);
		userRepository.saveAndFlush(user);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("ACCOUNT_LOCKED");
		assertFamilyRevoked(familyId);
	}

	@Test
	void accountLockedFlagCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("locked-flag@example.com", "Locked Flag Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		User user = userRepository.findById(session.userId()).orElseThrow();
		user.setAccountLocked(true);
		userRepository.saveAndFlush(user);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("ACCOUNT_LOCKED");
		assertFamilyRevoked(familyId);
	}

	@Test
	void disabledUserCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("disabled@example.com", "Disabled Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		User user = userRepository.findById(session.userId()).orElseThrow();
		user.setStatus(UserStatus.DISABLED);
		userRepository.saveAndFlush(user);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("ACCOUNT_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void unverifiedUserCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("unverified@example.com", "Unverified Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		User user = userRepository.findById(session.userId()).orElseThrow();
		user.setEmailVerified(false);
		userRepository.saveAndFlush(user);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("EMAIL_NOT_VERIFIED");
		assertFamilyRevoked(familyId);
	}

	@Test
	void suspendedMembershipCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("member-suspended@example.com", "Member Suspended Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		CompanyMember membership = membership(session);
		membership.setMembershipStatus(MembershipStatus.SUSPENDED);
		companyMemberRepository.saveAndFlush(membership);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("MEMBERSHIP_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void removedMembershipCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("member-removed@example.com", "Member Removed Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		CompanyMember membership = membership(session);
		membership.setMembershipStatus(MembershipStatus.REMOVED);
		companyMemberRepository.saveAndFlush(membership);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("MEMBERSHIP_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void inactiveMembershipCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("member-inactive@example.com", "Member Inactive Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		CompanyMember membership = membership(session);
		membership.setActive(false);
		companyMemberRepository.saveAndFlush(membership);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("MEMBERSHIP_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void membershipBelongingToAnotherUserIsRejectedAndRevokesFamily() {
		AuthSession session = verifiedLogin("member-other-user@example.com", "Member Other User Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		CompanyMember otherMembership = createOtherMembership(session.companyId());
		RefreshToken token = tokenRecord(session.refreshToken());
		token.setMembershipId(otherMembership.getId());
		refreshTokenRepository.saveAndFlush(token);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("MEMBERSHIP_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void membershipBelongingToAnotherCompanyIsRejectedAndRevokesFamily() {
		AuthSession session = verifiedLogin("member-other-company@example.com", "Member Other Company Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		Company otherCompany = createCompany("other-company-for-membership");
		CompanyMember otherCompanyMembership = new CompanyMember();
		otherCompanyMembership.setCompanyId(otherCompany.getId());
		otherCompanyMembership.setUserId(session.userId());
		otherCompanyMembership.setMembershipStatus(MembershipStatus.ACTIVE);
		otherCompanyMembership.setJoinedAt(Instant.now());
		otherCompanyMembership = companyMemberRepository.saveAndFlush(otherCompanyMembership);
		RefreshToken token = tokenRecord(session.refreshToken());
		token.setMembershipId(otherCompanyMembership.getId());
		refreshTokenRepository.saveAndFlush(token);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("MEMBERSHIP_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void suspendedCompanyCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("company-suspended@example.com", "Company Suspended Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		setCompanyStatus(session.companyId(), CompanyStatus.SUSPENDED);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("COMPANY_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void cancelledCompanyCannotRefreshAndRevokesFamily() {
		AuthSession session = verifiedLogin("company-cancelled@example.com", "Company Cancelled Co");
		UUID familyId = tokenRecord(session.refreshToken()).getFamilyId();
		setCompanyStatus(session.companyId(), CompanyStatus.CANCELLED);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(errorCode(refresh)).isEqualTo("COMPANY_NOT_ACTIVE");
		assertFamilyRevoked(familyId);
	}

	@Test
	void activeCompanyAllowsRefresh() {
		AuthSession session = verifiedLogin("company-active@example.com", "Company Active Co");
		setCompanyStatus(session.companyId(), CompanyStatus.ACTIVE);

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void trialCompanyAllowsRefresh() {
		AuthSession session = verifiedLogin("company-trial@example.com", "Company Trial Co");

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(authCompany(refresh).get("status")).isEqualTo("TRIAL");
	}

	@Test
	void roleRemovedAfterLoginIsRemovedFromRefreshedToken() {
		AuthSession session = verifiedLogin("role-removed@example.com", "Role Removed Co");
		assertThat(session.roles()).contains("OWNER");
		jdbcTemplate.update("update user_roles set active = false where company_id = ? and user_id = ?",
				session.companyId(), session.userId());

		ResponseEntity<Map> refresh = refresh(session.refreshToken());
		JwtPrincipal principal = jwtService.parseAccessToken(authValue(refresh, "accessToken"));

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(authList(refresh, "roles")).doesNotContain("OWNER");
		assertThat(principal.roles()).doesNotContain("OWNER");
	}

	@Test
	void permissionRemovedAfterLoginIsRemovedFromRefreshedToken() {
		AuthSession session = verifiedLogin("permission-removed@example.com", "Permission Removed Co");
		UUID roleId = replaceRolesWithCompanyRole(session, "REFRESH_PERMISSION_TEST", "COMPANY_VIEW");
		ResponseEntity<Map> loginAfterRoleChange = login("permission-removed@example.com");
		assertThat(authList(loginAfterRoleChange, "permissions")).contains("COMPANY_VIEW");
		jdbcTemplate.update("""
				delete from role_permissions
				where role_id = ?
				  and permission_id = (select id from permissions where code = 'COMPANY_VIEW')
				""", roleId);

		ResponseEntity<Map> refresh = refresh(authValue(loginAfterRoleChange, "refreshToken"));
		JwtPrincipal principal = jwtService.parseAccessToken(authValue(refresh, "accessToken"));

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(authList(refresh, "permissions")).doesNotContain("COMPANY_VIEW");
		assertThat(principal.permissions()).doesNotContain("COMPANY_VIEW");
	}

	@Test
	void validationFailureDoesNotCreateNewRefreshToken() {
		AuthSession session = verifiedLogin("no-new-token@example.com", "No New Token Co");
		CompanyMember membership = membership(session);
		membership.setMembershipStatus(MembershipStatus.SUSPENDED);
		companyMemberRepository.saveAndFlush(membership);
		long tokenCount = refreshTokenRepository.count();

		ResponseEntity<Map> refresh = refresh(session.refreshToken());

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(refreshTokenRepository.count()).isEqualTo(tokenCount);
	}

	@Test
	void validRefreshPreservesUserCompanyAndMembershipIds() {
		AuthSession session = verifiedLogin("ids-preserved@example.com", "Ids Preserved Co");

		ResponseEntity<Map> refresh = refresh(session.refreshToken());
		JwtPrincipal principal = jwtService.parseAccessToken(authValue(refresh, "accessToken"));
		RefreshToken rotatedToken = tokenRecord(authValue(refresh, "refreshToken"));

		assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(principal.userId()).isEqualTo(session.userId());
		assertThat(principal.companyId()).isEqualTo(session.companyId());
		assertThat(principal.membershipId()).isEqualTo(session.membershipId());
		assertThat(rotatedToken.getUserId()).isEqualTo(session.userId());
		assertThat(rotatedToken.getCompanyId()).isEqualTo(session.companyId());
		assertThat(rotatedToken.getMembershipId()).isEqualTo(session.membershipId());
	}

	private AuthSession verifiedLogin(String email, String companyName) {
		register(email, companyName);
		verify(latestVerificationToken());
		ResponseEntity<Map> login = login(email);
		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		CompanyMember membership = companyMemberRepository
				.findFirstByUserIdAndMembershipStatusAndActiveTrue(user.getId(), MembershipStatus.ACTIVE)
				.orElseThrow();
		return new AuthSession(
				user.getId(),
				membership.getCompanyId(),
				membership.getId(),
				authValue(login, "accessToken"),
				authValue(login, "refreshToken"),
				authList(login, "roles"),
				authList(login, "permissions")
		);
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

	private ResponseEntity<Map> verify(String token) {
		return restTemplate.postForEntity("/api/auth/verify-email", Map.of("token", token), Map.class);
	}

	private ResponseEntity<Map> login(String email) {
		return restTemplate.postForEntity("/api/auth/login", Map.of(
				"email", email,
				"password", "Password1"
		), Map.class);
	}

	private ResponseEntity<Map> refresh(String refreshToken) {
		try {
			String requestBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/refresh"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();
			HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			Map<String, Object> body = response.body() == null || response.body().isBlank()
					? Map.of()
					: objectMapper.readValue(response.body(), new TypeReference<>() {
					});
			return ResponseEntity.status(response.statusCode()).body(body);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to call refresh endpoint", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while calling refresh endpoint", ex);
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

	private RefreshToken tokenRecord(String rawToken) {
		return refreshTokenRepository.findByTokenHash(tokenService.hashToken(rawToken)).orElseThrow();
	}

	private CompanyMember membership(AuthSession session) {
		return companyMemberRepository.findById(session.membershipId()).orElseThrow();
	}

	private CompanyMember createOtherMembership(UUID companyId) {
		User user = new User();
		user.setFirstName("Other");
		user.setLastName("User");
		user.setEmail("other-" + UUID.randomUUID() + "@example.com");
		user.setPasswordHash("not-used");
		user.setEmailVerified(true);
		user.setAccountLocked(false);
		user.setStatus(UserStatus.ACTIVE);
		user = userRepository.saveAndFlush(user);

		CompanyMember membership = new CompanyMember();
		membership.setCompanyId(companyId);
		membership.setUserId(user.getId());
		membership.setMembershipStatus(MembershipStatus.ACTIVE);
		membership.setJoinedAt(Instant.now());
		return companyMemberRepository.saveAndFlush(membership);
	}

	private Company createCompany(String slug) {
		Company company = new Company();
		company.setName(slug);
		company.setSlug(slug);
		company.setEmail(slug + "@example.com");
		company.setCountry("India");
		company.setTimezone("Asia/Kolkata");
		company.setCurrency("INR");
		company.setStatus(CompanyStatus.ACTIVE);
		return companyRepository.saveAndFlush(company);
	}

	private void setCompanyStatus(UUID companyId, CompanyStatus status) {
		Company company = companyRepository.findById(companyId).orElseThrow();
		company.setStatus(status);
		companyRepository.saveAndFlush(company);
	}

	private UUID replaceRolesWithCompanyRole(AuthSession session, String roleName, String permissionCode) {
		UUID roleId = jdbcTemplate.queryForObject("""
				insert into roles (company_id, name, description, protected_system_role, active)
				values (?, ?, 'Refresh test role', false, true)
				returning id
				""", UUID.class, session.companyId(), roleName);
		jdbcTemplate.update("""
				insert into role_permissions (role_id, permission_id)
				select ?, id from permissions where code = ?
				""", roleId, permissionCode);
		jdbcTemplate.update("delete from user_roles where company_id = ? and user_id = ?",
				session.companyId(), session.userId());
		jdbcTemplate.update("""
				insert into user_roles (company_id, membership_id, user_id, role_id, active)
				values (?, ?, ?, ?, true)
				""", session.companyId(), session.membershipId(), session.userId(), roleId);
		return roleId;
	}

	private void assertFamilyRevoked(UUID familyId) {
		assertThat(refreshTokenRepository.findByFamilyId(familyId))
				.isNotEmpty()
				.allSatisfy(token -> {
					assertThat(token.getActive()).isFalse();
					assertThat(token.getRevokedAt()).isNotNull();
				});
	}

	private String authValue(ResponseEntity<Map> response, String key) {
		return (String) authData(response).get(key);
	}

	@SuppressWarnings("unchecked")
	private List<String> authList(ResponseEntity<Map> response, String key) {
		return (List<String>) authData(response).get(key);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> authCompany(ResponseEntity<Map> response) {
		return (Map<String, Object>) authData(response).get("company");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> authData(ResponseEntity<Map> response) {
		return (Map<String, Object>) response.getBody().get("data");
	}

	private String errorCode(ResponseEntity<Map> response) {
		return (String) response.getBody().get("code");
	}

	record AuthSession(
			UUID userId,
			UUID companyId,
			UUID membershipId,
			String accessToken,
			String refreshToken,
			List<String> roles,
			List<String> permissions
	) {
	}

}
