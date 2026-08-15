package com.vednex.business_suite.tenant;

import java.util.List;
import java.util.UUID;

import com.vednex.business_suite.common.exception.ForbiddenException;
import com.vednex.business_suite.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

	public JwtPrincipal currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
			throw new ForbiddenException("TENANT_CONTEXT_MISSING", "Tenant context is not available");
		}
		return principal;
	}

	public UUID userId() {
		return currentPrincipal().userId();
	}

	public UUID companyId() {
		return currentPrincipal().companyId();
	}

	public UUID membershipId() {
		return currentPrincipal().membershipId();
	}

	public List<String> roles() {
		return currentPrincipal().roles();
	}

	public List<String> permissions() {
		return currentPrincipal().permissions();
	}

}
