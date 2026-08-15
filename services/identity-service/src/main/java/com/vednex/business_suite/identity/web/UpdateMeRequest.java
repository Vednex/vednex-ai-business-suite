package com.vednex.business_suite.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
		@NotBlank
		@Size(max = 100)
		String firstName,

		@NotBlank
		@Size(max = 100)
		String lastName,

		@Size(max = 40)
		String phone,

		@Size(max = 1000)
		String profileImageUrl
) {
}
