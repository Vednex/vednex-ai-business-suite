package com.vednex.business_suite.security.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vednex.business_suite.common.exception.BusinessRuleException;
import com.vednex.business_suite.common.exception.NotFoundException;
import com.vednex.business_suite.security.PermissionService;
import com.vednex.business_suite.security.domain.Permission;
import com.vednex.business_suite.security.domain.Role;
import com.vednex.business_suite.security.domain.RolePermission;
import com.vednex.business_suite.security.repository.PermissionRepository;
import com.vednex.business_suite.security.repository.RolePermissionRepository;
import com.vednex.business_suite.security.repository.RoleRepository;
import com.vednex.business_suite.security.repository.UserRoleRepository;
import com.vednex.business_suite.security.web.PermissionResponse;
import com.vednex.business_suite.security.web.RoleRequest;
import com.vednex.business_suite.security.web.RoleResponse;
import com.vednex.business_suite.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleManagementService {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRoleRepository userRoleRepository;
	private final PermissionService permissionService;
	private final TenantContext tenantContext;

	public RoleManagementService(RoleRepository roleRepository, PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository, UserRoleRepository userRoleRepository,
			PermissionService permissionService, TenantContext tenantContext) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.userRoleRepository = userRoleRepository;
		this.permissionService = permissionService;
		this.tenantContext = tenantContext;
	}

	@Transactional(readOnly = true)
	public List<RoleResponse> listRoles() {
		permissionService.require("ROLE_VIEW");
		return roleRepository.findVisibleRoles(tenantContext.companyId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public RoleResponse create(RoleRequest request) {
		permissionService.require("ROLE_CREATE");
		Role role = new Role();
		role.setCompanyId(tenantContext.companyId());
		role.setName(request.name().trim());
		role.setDescription(blankToNull(request.description()));
		role.setProtectedSystemRole(false);
		role.setCreatedBy(tenantContext.userId());
		role = roleRepository.save(role);
		replacePermissions(role.getId(), request.permissions());
		return toResponse(role);
	}

	@Transactional(readOnly = true)
	public RoleResponse get(UUID roleId) {
		permissionService.require("ROLE_VIEW");
		return toResponse(loadVisibleRole(roleId));
	}

	@Transactional
	public RoleResponse update(UUID roleId, RoleRequest request) {
		permissionService.require("ROLE_UPDATE");
		Role role = loadVisibleRole(roleId);
		role.setName(request.name().trim());
		role.setDescription(blankToNull(request.description()));
		role.setUpdatedBy(tenantContext.userId());
		replacePermissions(role.getId(), request.permissions());
		return toResponse(role);
	}

	@Transactional
	public void delete(UUID roleId) {
		permissionService.require("ROLE_UPDATE");
		Role role = loadVisibleRole(roleId);
		if (Boolean.TRUE.equals(role.getProtectedSystemRole())) {
			throw new BusinessRuleException("PROTECTED_ROLE", "Protected system roles cannot be deleted");
		}
		if (userRoleRepository.existsByRoleIdAndActiveTrue(roleId)) {
			throw new BusinessRuleException("ROLE_ASSIGNED", "Cannot delete a role that is assigned to users");
		}
		role.setActive(false);
		role.setUpdatedBy(tenantContext.userId());
	}

	@Transactional(readOnly = true)
	public List<PermissionResponse> listPermissions() {
		permissionService.require("ROLE_VIEW");
		return permissionRepository.findByActiveTrueOrderByCode().stream()
				.map(permission -> new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription()))
				.toList();
	}

	private Role loadVisibleRole(UUID roleId) {
		return roleRepository.findVisibleRole(roleId, tenantContext.companyId())
				.orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
	}

	private void replacePermissions(UUID roleId, List<String> requestedPermissions) {
		List<String> normalized = requestedPermissions.stream().map(String::trim).distinct().toList();
		List<Permission> permissions = permissionRepository.findByCodeInAndActiveTrue(normalized);
		Set<String> foundCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
		for (String code : normalized) {
			if (!foundCodes.contains(code)) {
				throw new NotFoundException("PERMISSION_NOT_FOUND", "Permission not found: " + code);
			}
		}
		rolePermissionRepository.deleteByRoleId(roleId);
		for (Permission permission : permissions) {
			RolePermission rolePermission = new RolePermission();
			rolePermission.setRoleId(roleId);
			rolePermission.setPermissionId(permission.getId());
			rolePermissionRepository.save(rolePermission);
		}
	}

	private RoleResponse toResponse(Role role) {
		return new RoleResponse(role.getId(), role.getCompanyId(), role.getName(), role.getDescription(),
				Boolean.TRUE.equals(role.getProtectedSystemRole()), rolePermissionRepository.findPermissionCodes(role.getId()));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
