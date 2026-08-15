package com.vednex.business_suite.security;

import com.vednex.business_suite.common.exception.ForbiddenException;
import com.vednex.business_suite.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

	private final TenantContext tenantContext;

	public PermissionService(TenantContext tenantContext) {
		this.tenantContext = tenantContext;
	}

	public void require(String permission) {
		if (!tenantContext.permissions().contains(permission)) {
			throw new ForbiddenException("PERMISSION_DENIED", "You do not have permission to perform this action");
		}
	}

	public boolean has(String permission) {
		return tenantContext.permissions().contains(permission);
	}

}
