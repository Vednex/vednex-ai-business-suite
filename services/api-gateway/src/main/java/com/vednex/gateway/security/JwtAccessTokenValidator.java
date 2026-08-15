package com.vednex.gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenValidator {

	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final PublicKey publicKey;
	private final String issuer;
	private final String audience;

	public JwtAccessTokenValidator(
			ObjectMapper objectMapper,
			Clock clock,
			@Value("${gateway.jwt.public-key}") String publicKey,
			@Value("${gateway.jwt.public-key-file}") String publicKeyFile,
			@Value("${gateway.jwt.issuer}") String issuer,
			@Value("${gateway.jwt.audience}") String audience
	) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.publicKey = RsaKeyLoader.publicKey(publicKey, publicKeyFile);
		this.issuer = requireValue(issuer, "gateway.jwt.issuer");
		this.audience = requireValue(audience, "gateway.jwt.audience");
	}

	public void validateAccessToken(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new JwtValidationException();
			}
			Map<String, Object> header = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[0]), MAP_TYPE);
			if (!"RS256".equals(header.get("alg"))) {
				throw new JwtValidationException();
			}
			if (!verifySignature(parts[0] + "." + parts[1], parts[2])) {
				throw new JwtValidationException();
			}

			Map<String, Object> claims = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), MAP_TYPE);
			if (!"access".equals(claims.get("type"))) {
				throw new JwtValidationException();
			}
			if (!issuer.equals(stringClaim(claims, "iss"))) {
				throw new JwtValidationException();
			}
			if (!audience.equals(stringClaim(claims, "aud"))) {
				throw new JwtValidationException();
			}
			Number expiresAt = numberClaim(claims, "exp");
			if (!Instant.ofEpochSecond(expiresAt.longValue()).isAfter(Instant.now(clock))) {
				throw new JwtValidationException();
			}
			stringClaim(claims, "sub");
			stringClaim(claims, "companyId");
			stringClaim(claims, "membershipId");
			listClaim(claims, "roles");
			listClaim(claims, "permissions");
		}
		catch (JwtValidationException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new JwtValidationException();
		}
	}

	private boolean verifySignature(String signingInput, String encodedSignature) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(publicKey);
		signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
		return signature.verify(BASE64_URL_DECODER.decode(encodedSignature));
	}

	private String stringClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new JwtValidationException();
	}

	private Number numberClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof Number numberValue) {
			return numberValue;
		}
		throw new JwtValidationException();
	}

	private void listClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof Iterable<?>) {
			return;
		}
		throw new JwtValidationException();
	}

	private String requireValue(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " must be configured");
		}
		return value.trim();
	}

	public static class JwtValidationException extends RuntimeException {
	}

}
