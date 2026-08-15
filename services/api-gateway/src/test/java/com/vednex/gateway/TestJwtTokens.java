package com.vednex.gateway;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

final class TestJwtTokens {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final KeyPair KEY_PAIR = generate();
	private static final KeyPair WRONG_KEY_PAIR = generate();

	private TestJwtTokens() {
	}

	static String publicKey() {
		return pem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded());
	}

	static String validToken() {
		return token(KEY_PAIR, Instant.now().plusSeconds(900), "access");
	}

	static String expiredToken() {
		return token(KEY_PAIR, Instant.now().minusSeconds(60), "access");
	}

	static String refreshTypeToken() {
		return token(KEY_PAIR, Instant.now().plusSeconds(900), "refresh");
	}

	static String wrongSignatureToken() {
		return token(WRONG_KEY_PAIR, Instant.now().plusSeconds(900), "access");
	}

	private static String token(KeyPair keyPair, Instant expiresAt, String type) {
		try {
			Map<String, Object> claims = new LinkedHashMap<>();
			claims.put("sub", UUID.randomUUID().toString());
			claims.put("companyId", UUID.randomUUID().toString());
			claims.put("membershipId", UUID.randomUUID().toString());
			claims.put("roles", List.of("OWNER"));
			claims.put("permissions", List.of("COMPANY_VIEW", "USER_INVITE"));
			claims.put("type", type);
			claims.put("iat", Instant.now().getEpochSecond());
			claims.put("exp", expiresAt.getEpochSecond());
			claims.put("iss", "vednex-identity-service");
			claims.put("aud", "vednex-business-suite");

			String header = encodeJson(Map.of("alg", "RS256", "typ", "JWT"));
			String payload = encodeJson(claims);
			String signingInput = header + "." + payload;
			return signingInput + "." + sign(keyPair, signingInput);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create test JWT", ex);
		}
	}

	private static String encodeJson(Object value) throws Exception {
		return BASE64_URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
	}

	private static String sign(KeyPair keyPair, String signingInput) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(keyPair.getPrivate());
		signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
		return BASE64_URL_ENCODER.encodeToString(signature.sign());
	}

	private static KeyPair generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to generate test RSA key pair", ex);
		}
	}

	private static String pem(String type, byte[] der) {
		return "-----BEGIN " + type + "-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
				+ "\n-----END " + type + "-----";
	}

}
