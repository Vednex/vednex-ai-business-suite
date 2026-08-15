package com.vednex.business_suite.company.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record AssignRolesRequest(
		@NotEmpty
		List<UUID> roleIds
) {
}
