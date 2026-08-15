package com.vednex.business_suite.company.web;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
		@NotBlank
		@Email
		String email,

		@NotNull
		UUID roleId
) {
}
