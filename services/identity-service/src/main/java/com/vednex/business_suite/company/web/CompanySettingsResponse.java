package com.vednex.business_suite.company.web;

public record CompanySettingsResponse(
		String logoUrl,
		String addressLine1,
		String addressLine2,
		String city,
		String state,
		String postalCode,
		String country,
		String gstNumber,
		String taxNumber,
		String currency,
		String timezone,
		String language,
		String dateFormat
) {
}
