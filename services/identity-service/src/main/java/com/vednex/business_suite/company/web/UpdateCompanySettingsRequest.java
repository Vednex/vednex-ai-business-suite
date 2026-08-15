package com.vednex.business_suite.company.web;

import jakarta.validation.constraints.Size;

public record UpdateCompanySettingsRequest(
		@Size(max = 1000)
		String logoUrl,
		@Size(max = 200)
		String addressLine1,
		@Size(max = 200)
		String addressLine2,
		@Size(max = 100)
		String city,
		@Size(max = 100)
		String state,
		@Size(max = 30)
		String postalCode,
		@Size(max = 80)
		String country,
		@Size(max = 80)
		String gstNumber,
		@Size(max = 80)
		String taxNumber,
		@Size(min = 3, max = 3)
		String currency,
		@Size(max = 80)
		String timezone,
		@Size(max = 20)
		String language,
		@Size(max = 40)
		String dateFormat
) {
}
