package com.vednex.business_suite.security.web;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
		UUID id,
		UUID companyId,
		String name,
		String description,
		boolean protectedSystemRole,
		List<String> permissions
) {
}
