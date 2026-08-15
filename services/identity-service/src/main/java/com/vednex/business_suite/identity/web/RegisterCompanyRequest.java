package com.vednex.business_suite.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCompanyRequest(
		@NotBlank
		@Size(max = 160)
		String companyName,

		@Size(max = 120)
		@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase URL slug")
		String companySlug,

		@Email
		@Size(max = 320)
		String companyEmail,

		@NotBlank
		@Size(max = 80)
		String country,

		@NotBlank
		@Size(max = 80)
		String timezone,

		@Size(min = 3, max = 3)
		String currency,

		@NotBlank
		@Size(max = 100)
		String firstName,

		@NotBlank
		@Size(max = 100)
		String lastName,

		@NotBlank
		@Email
		@Size(max = 320)
		String email,

		@NotBlank
		@Size(min = 8, max = 100)
		String password
) {
}
