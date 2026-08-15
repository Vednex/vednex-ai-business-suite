package com.vednex.business_suite.identity.web;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
		@NotBlank
		String refreshToken
) {
}
