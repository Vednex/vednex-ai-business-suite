package com.vednex.business_suite.identity.web;

import java.util.UUID;

public record RegistrationResponse(
		UUID userId,
		UUID companyId,
		String email,
		String companySlug,
		boolean emailVerificationRequired
) {
}
