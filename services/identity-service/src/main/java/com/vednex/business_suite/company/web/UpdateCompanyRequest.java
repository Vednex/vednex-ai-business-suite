package com.vednex.business_suite.company.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
		@NotBlank
		@Size(max = 160)
		String name,

		@Size(max = 200)
		String legalName,

		@NotBlank
		@Email
		@Size(max = 320)
		String email,

		@Size(max = 40)
		String phone,

		@NotBlank
		@Size(max = 80)
		String country,

		@NotBlank
		@Size(max = 80)
		String timezone,

		@NotBlank
		@Size(min = 3, max = 3)
		String currency
) {
}
