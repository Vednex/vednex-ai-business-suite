package com.vednex.business_suite.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessViolationException extends ApiException {
	public TenantAccessViolationException() {
		super("TENANT_ACCESS_DENIED", "The requested resource is not available in the current tenant", HttpStatus.FORBIDDEN);
	}
}
