package com.vednex.gateway.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaKeyLoader {

	private RsaKeyLoader() {
	}

	static PublicKey publicKey(String keyValue, String keyFile) {
		try {
			String material = keyMaterial(keyValue, keyFile);
			byte[] der = keyBytes(material);
			return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load JWT public key", ex);
		}
	}

	private static String keyMaterial(String keyValue, String keyFile) throws Exception {
		if (keyValue != null && !keyValue.isBlank()) {
			return keyValue;
		}
		if (keyFile != null && !keyFile.isBlank()) {
			return Files.readString(Path.of(keyFile.trim()));
		}
		throw new IllegalStateException("JWT public key is not configured");
	}

	private static byte[] keyBytes(String material) {
		String normalized = material.trim().replace("\\n", "\n");
		if (normalized.contains("-----BEGIN")) {
			String base64 = normalized
					.replaceAll("-----BEGIN [^-]+-----", "")
					.replaceAll("-----END [^-]+-----", "")
					.replaceAll("\\s", "");
			return Base64.getDecoder().decode(base64);
		}

		byte[] decoded = Base64.getDecoder().decode(normalized.replaceAll("\\s", ""));
		String decodedText = new String(decoded, StandardCharsets.UTF_8).trim();
		if (decodedText.contains("-----BEGIN")) {
			return keyBytes(decodedText);
		}
		return decoded;
	}

}
