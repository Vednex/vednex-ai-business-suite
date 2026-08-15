package com.vednex.business_suite.identity.web;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		@NotBlank
		String refreshToken
) {
}
