package com.vednex.business_suite.company.web;

import com.vednex.business_suite.company.domain.MembershipStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
		@NotNull
		MembershipStatus status
) {
}
