package com.vednex.business_suite.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vednex.business_suite.common.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-06T00:00:00Z"), ZoneOffset.UTC);
	private static final String ISSUER = "vednex-identity-service";
	private static final String AUDIENCE = "vednex-business-suite";

	@Test
	void createsAndParsesAccessToken() {
		JwtService jwtService = jwtService(CLOCK);
		JwtPrincipal principal = new JwtPrincipal(
				UUID.randomUUID(),
				null,
				UUID.randomUUID(),
				UUID.randomUUID(),
				List.of("OWNER"),
				List.of("COMPANY_VIEW", "USER_INVITE")
		);

		JwtPrincipal parsed = jwtService.parseAccessToken(jwtService.createAccessToken(principal));

		assertThat(parsed).isEqualTo(principal);
	}

	@Test
	void accessTokenUsesAsymmetricSignatureAndRequiredClaims() throws Exception {
		JwtService jwtService = jwtService(CLOCK);
		UUID userId = UUID.randomUUID();
		UUID companyId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		JwtPrincipal principal = new JwtPrincipal(
				userId,
				"owner@example.com",
				companyId,
				membershipId,
				List.of("OWNER"),
				List.of("COMPANY_VIEW", "USER_INVITE")
		);

		String token = jwtService.createAccessToken(principal);
		Map<String, Object> header = readPart(token, 0);
		Map<String, Object> claims = readPart(token, 1);

		assertThat(header).containsEntry("alg", "RS256").containsEntry("typ", "JWT");
		assertThat(claims).containsEntry("sub", userId.toString());
		assertThat(claims).containsEntry("companyId", companyId.toString());
		assertThat(claims).containsEntry("membershipId", membershipId.toString());
		assertThat(claims).containsEntry("type", "access");
		assertThat(claims).containsEntry("iss", ISSUER);
		assertThat(claims).containsEntry("aud", AUDIENCE);
		assertThat(claims.get("roles")).isEqualTo(List.of("OWNER"));
		assertThat(claims.get("permissions")).isEqualTo(List.of("COMPANY_VIEW", "USER_INVITE"));
		assertThat(claims).containsKeys("iat", "exp");
		assertThat(jwtService.parseAccessToken(token))
				.isEqualTo(new JwtPrincipal(userId, null, companyId, membershipId,
						List.of("OWNER"), List.of("COMPANY_VIEW", "USER_INVITE")));
	}

	@Test
	void rejectsTamperedToken() {
		JwtService jwtService = jwtService(CLOCK);
		JwtPrincipal principal = new JwtPrincipal(
				UUID.randomUUID(),
				"owner@example.com",
				UUID.randomUUID(),
				UUID.randomUUID(),
				List.of("OWNER"),
				List.of("COMPANY_VIEW")
		);
		String token = jwtService.createAccessToken(principal);
		String tamperedToken = token.substring(0, token.length() - 2) + "xx";

		assertThatThrownBy(() -> jwtService.parseAccessToken(tamperedToken))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsExpiredToken() {
		JwtPrincipal principal = principal();
		String token = jwtService(CLOCK).createAccessToken(principal);
		Clock later = Clock.fixed(Instant.parse("2026-08-06T00:16:00Z"), ZoneOffset.UTC);

		assertThatThrownBy(() -> jwtService(later).parseAccessToken(token))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsWrongPublicKey() {
		JwtPrincipal principal = principal();
		String token = jwtService(CLOCK).createAccessToken(principal);
		JwtService wrongPublicKeyService = new JwtService(
				OBJECT_MAPPER,
				CLOCK,
				TestRsaKeys.privateKey(),
				"",
				TestRsaKeys.wrongPublicKey(),
				"",
				ISSUER,
				AUDIENCE,
				15
		);

		assertThatThrownBy(() -> wrongPublicKeyService.parseAccessToken(token))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void rejectsRefreshTokenAsAccessToken() {
		assertThatThrownBy(() -> jwtService(CLOCK).parseAccessToken("opaque-refresh-token"))
				.isInstanceOf(InvalidTokenException.class);
	}

	private JwtService jwtService(Clock clock) {
		return new JwtService(
				OBJECT_MAPPER,
				clock,
				TestRsaKeys.privateKey(),
				"",
				TestRsaKeys.publicKey(),
				"",
				ISSUER,
				AUDIENCE,
				15
		);
	}

	private JwtPrincipal principal() {
		return new JwtPrincipal(
				UUID.randomUUID(),
				"owner@example.com",
				UUID.randomUUID(),
				UUID.randomUUID(),
				List.of("OWNER"),
				List.of("COMPANY_VIEW")
		);
	}

	private Map<String, Object> readPart(String token, int index) throws Exception {
		String[] parts = token.split("\\.");
		byte[] json = Base64.getUrlDecoder().decode(parts[index]);
		return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
		});
	}

}
