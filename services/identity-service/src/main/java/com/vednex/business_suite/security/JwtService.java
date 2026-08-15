package com.vednex.business_suite.security;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vednex.business_suite.common.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private final String issuer;
	private final String audience;
	private final long accessExpirationMinutes;

	public JwtService(
			ObjectMapper objectMapper,
			Clock clock,
			@Value("${app.jwt.private-key}") String privateKey,
			@Value("${app.jwt.private-key-file}") String privateKeyFile,
			@Value("${app.jwt.public-key}") String publicKey,
			@Value("${app.jwt.public-key-file}") String publicKeyFile,
			@Value("${app.jwt.issuer}") String issuer,
			@Value("${app.jwt.audience}") String audience,
			@Value("${app.jwt.access-expiration-minutes}") long accessExpirationMinutes
	) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.privateKey = RsaKeyLoader.privateKey(privateKey, privateKeyFile);
		this.publicKey = RsaKeyLoader.publicKey(publicKey, publicKeyFile);
		this.issuer = requireValue(issuer, "app.jwt.issuer");
		this.audience = requireValue(audience, "app.jwt.audience");
		this.accessExpirationMinutes = accessExpirationMinutes;
	}

	public String createAccessToken(JwtPrincipal principal) {
		Instant now = Instant.now(clock);
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", principal.userId().toString());
		claims.put("companyId", principal.companyId().toString());
		claims.put("membershipId", principal.membershipId().toString());
		claims.put("roles", principal.roles());
		claims.put("permissions", principal.permissions());
		claims.put("type", "access");
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", now.plusSeconds(accessExpirationMinutes * 60).getEpochSecond());
		claims.put("iss", issuer);
		claims.put("aud", audience);
		return sign(claims);
	}

	public JwtPrincipal parseAccessToken(String token) {
		Map<String, Object> claims = verifyAndReadClaims(token);
		if (!"access".equals(claims.get("type"))) {
			throw new InvalidTokenException("Invalid token type");
		}
		Number expiresAt = numberClaim(claims, "exp");
		if (Instant.ofEpochSecond(expiresAt.longValue()).isBefore(Instant.now(clock))) {
			throw new InvalidTokenException("Token has expired");
		}
		if (!issuer.equals(stringClaim(claims, "iss"))) {
			throw new InvalidTokenException("Invalid token issuer");
		}
		if (!audience.equals(stringClaim(claims, "aud"))) {
			throw new InvalidTokenException("Invalid token audience");
		}
		return new JwtPrincipal(
				UUID.fromString(stringClaim(claims, "sub")),
				null,
				UUID.fromString(stringClaim(claims, "companyId")),
				UUID.fromString(stringClaim(claims, "membershipId")),
				stringListClaim(claims, "roles"),
				stringListClaim(claims, "permissions")
		);
	}

	private String sign(Map<String, Object> claims) {
		try {
			String header = encodeJson(Map.of("alg", "RS256", "typ", "JWT"));
			String payload = encodeJson(claims);
			String signingInput = header + "." + payload;
			return signingInput + "." + rsaSha256(signingInput);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create access token", ex);
		}
	}

	private Map<String, Object> verifyAndReadClaims(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new InvalidTokenException("Malformed token");
			}
			Map<String, Object> header = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[0]), MAP_TYPE);
			if (!"RS256".equals(header.get("alg"))) {
				throw new InvalidTokenException("Invalid token algorithm");
			}
			String signingInput = parts[0] + "." + parts[1];
			if (!verifyRsaSha256(signingInput, parts[2])) {
				throw new InvalidTokenException("Invalid token signature");
			}
			byte[] payload = BASE64_URL_DECODER.decode(parts[1]);
			return objectMapper.readValue(payload, MAP_TYPE);
		}
		catch (InvalidTokenException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new InvalidTokenException("Invalid token");
		}
	}

	private String encodeJson(Object value) throws Exception {
		return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
	}

	private String rsaSha256(String signingInput) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);
		signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
		return BASE64_URL_ENCODER.encodeToString(signature.sign());
	}

	private boolean verifyRsaSha256(String signingInput, String encodedSignature) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(publicKey);
		signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
		return signature.verify(BASE64_URL_DECODER.decode(encodedSignature));
	}

	private String requireValue(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " must be configured");
		}
		return value.trim();
	}

	private String stringClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new InvalidTokenException("Missing token claim: " + name);
	}

	private Number numberClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof Number numberValue) {
			return numberValue;
		}
		throw new InvalidTokenException("Missing token claim: " + name);
	}

	private List<String> stringListClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof List<?> values) {
			return values.stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.toList();
		}
		throw new InvalidTokenException("Missing token claim: " + name);
	}

}
