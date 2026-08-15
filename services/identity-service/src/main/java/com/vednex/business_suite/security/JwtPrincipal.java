package com.vednex.business_suite.security;

import java.util.List;
import java.util.UUID;

public record JwtPrincipal(
		UUID userId,
		String email,
		UUID companyId,
		UUID membershipId,
		List<String> roles,
		List<String> permissions
) {
}
