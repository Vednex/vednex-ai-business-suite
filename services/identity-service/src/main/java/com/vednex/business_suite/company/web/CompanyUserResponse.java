package com.vednex.business_suite.company.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyUserResponse(
		UUID userId,
		UUID membershipId,
		String firstName,
		String lastName,
		String email,
		String profileImageUrl,
		String status,
		String userStatus,
		String membershipStatus,
		String jobTitle,
		String departmentName,
		Instant joinedAt,
		List<String> roles
) {
}
