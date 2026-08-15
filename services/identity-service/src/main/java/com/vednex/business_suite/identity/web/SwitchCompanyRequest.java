package com.vednex.business_suite.identity.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SwitchCompanyRequest(
		@NotNull
		UUID companyId,

		String refreshToken
) {
}
