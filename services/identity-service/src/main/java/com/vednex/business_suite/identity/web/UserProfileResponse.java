package com.vednex.business_suite.identity.web;

import java.util.UUID;

public record UserProfileResponse(
		UUID id,
		String firstName,
		String lastName,
		String email,
		String phone,
		String profileImageUrl,
		boolean emailVerified,
		String status
) {
}
