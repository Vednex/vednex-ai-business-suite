package com.vednex.business_suite.company.web;

import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
		@Size(max = 100)
		String firstName,

		@Size(max = 100)
		String lastName,

		@Size(min = 8, max = 100)
		String password
) {
}
