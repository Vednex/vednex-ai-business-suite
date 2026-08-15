package com.vednex.business_suite.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestRsaKeys {

	private static final KeyPair KEY_PAIR = generate();
	private static final KeyPair WRONG_KEY_PAIR = generate();

	private TestRsaKeys() {
	}

	public static String privateKey() {
		return pem("PRIVATE KEY", KEY_PAIR.getPrivate().getEncoded());
	}

	public static String publicKey() {
		return pem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded());
	}

	public static String wrongPublicKey() {
		return pem("PUBLIC KEY", WRONG_KEY_PAIR.getPublic().getEncoded());
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
