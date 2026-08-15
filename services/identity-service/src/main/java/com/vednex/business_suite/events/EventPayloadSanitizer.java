package com.vednex.business_suite.events;

import java.util.Locale;
import java.util.Map;

final class EventPayloadSanitizer {

	private static final String[] FORBIDDEN_KEY_PARTS = {
			"password",
			"token",
			"secret",
			"privatekey",
			"private_key",
			"authorization",
			"credential"
	};

	private EventPayloadSanitizer() {
	}

	static Map<String, Object> requireSafe(Map<String, Object> data) {
		Map<String, Object> safeData = data == null ? Map.of() : data;
		safeData.keySet().forEach(EventPayloadSanitizer::validateKey);
		return Map.copyOf(safeData);
	}

	private static void validateKey(String key) {
		String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("-", "").replace(".", "");
		for (String forbidden : FORBIDDEN_KEY_PARTS) {
			if (normalized.contains(forbidden)) {
				throw new IllegalArgumentException("Event payload contains forbidden key: " + key);
			}
		}
	}
}
