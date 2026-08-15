package com.vednex.business_suite.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
		@NotBlank
		@Email
		String email
) {
}
