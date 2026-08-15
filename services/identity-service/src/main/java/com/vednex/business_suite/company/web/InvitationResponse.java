package com.vednex.business_suite.company.web;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
		UUID id,
		String email,
		UUID roleId,
		String status,
		Instant expiresAt,
		Instant acceptedAt,
		Instant revokedAt
) {
}
