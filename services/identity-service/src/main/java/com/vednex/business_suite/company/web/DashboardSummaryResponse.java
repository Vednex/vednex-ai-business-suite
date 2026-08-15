package com.vednex.business_suite.company.web;

import java.time.Instant;

public record DashboardSummaryResponse(
		String companyName,
		String currentPlan,
		Instant trialExpiry,
		long totalCompanyUsers,
		long activeUsers,
		long pendingInvitations
) {
}
