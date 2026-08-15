package com.vednex.business_suite.identity.web;

import java.util.List;
import java.util.UUID;

public record MeResponse(
		UUID userId,
		String email,
		UUID companyId,
		UUID membershipId,
		List<String> roles,
		List<String> permissions
) {
}
