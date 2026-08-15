package com.vednex.business_suite.company.web;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
		UUID id,
		String name,
		String legalName,
		String slug,
		String email,
		String phone,
		String country,
		String timezone,
		String currency,
		String status,
		Instant trialEndsAt
) {
}
