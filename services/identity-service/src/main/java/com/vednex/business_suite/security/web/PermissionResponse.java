package com.vednex.business_suite.security.web;

import java.util.UUID;

public record PermissionResponse(
		UUID id,
		String code,
		String description
) {
}
