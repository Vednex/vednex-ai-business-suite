package com.vednex.business_suite.identity.web;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresInSeconds,
		AuthenticatedUserResponse user,
		AuthenticatedCompanyResponse company,
		List<String> roles,
		List<String> permissions
) {

	public static AuthResponse bearer(
			String accessToken,
			String refreshToken,
			long expiresInSeconds,
			AuthenticatedUserResponse user,
			AuthenticatedCompanyResponse company,
			List<String> roles,
			List<String> permissions
	) {
		return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user, company, roles, permissions);
	}

	public record AuthenticatedUserResponse(
			UUID id,
			String firstName,
			String lastName,
			String email,
			boolean emailVerified
	) {
	}

	public record AuthenticatedCompanyResponse(
			UUID id,
			String name,
			String slug,
			String status
	) {
	}

}
