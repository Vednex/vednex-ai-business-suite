package com.vednex.business_suite.security.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleRequest(
		@NotBlank
		@Size(max = 80)
		@Pattern(regexp = "^[A-Z0-9_]+$", message = "must use uppercase letters, numbers, and underscores")
		String name,

		@Size(max = 255)
		String description,

		@NotNull
		List<String> permissions
) {
}
