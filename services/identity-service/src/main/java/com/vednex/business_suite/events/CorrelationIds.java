package com.vednex.business_suite.events;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

public final class CorrelationIds {

	private static final String PRIMARY_HEADER = "X-Correlation-ID";
	private static final String COMPATIBILITY_HEADER = "X-Correlation-Id";

	private CorrelationIds() {
	}

	public static UUID from(HttpServletRequest request) {
		if (request != null) {
			UUID parsed = parse(request.getHeader(PRIMARY_HEADER));
			if (parsed != null) {
				return parsed;
			}
			parsed = parse(request.getHeader(COMPATIBILITY_HEADER));
			if (parsed != null) {
				return parsed;
			}
		}
		return UUID.randomUUID();
	}

	public static UUID from(String value) {
		UUID parsed = parse(value);
		return parsed == null ? UUID.randomUUID() : parsed;
	}

	private static UUID parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value.trim());
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
