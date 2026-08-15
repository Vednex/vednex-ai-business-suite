package com.vednex.business_suite.company.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vednex.business_suite.audit.AuditAction;
import com.vednex.business_suite.audit.AuditService;
import com.vednex.business_suite.common.exception.BusinessRuleException;
import com.vednex.business_suite.common.exception.NotFoundException;
import com.vednex.business_suite.company.domain.CompanyMember;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.web.AssignRolesRequest;
import com.vednex.business_suite.company.web.CompanyUserResponse;
import com.vednex.business_suite.company.web.UpdateUserStatusRequest;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.security.PermissionService;
import com.vednex.business_suite.security.domain.Role;
import com.vednex.business_suite.security.domain.UserRole;
import com.vednex.business_suite.security.repository.RoleRepository;
import com.vednex.business_suite.security.repository.UserRoleRepository;
import com.vednex.business_suite.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyUserService {

	private final CompanyMemberRepository companyMemberRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final PermissionService permissionService;
	private final TenantContext tenantContext;
	private final AuditService auditService;

	public CompanyUserService(CompanyMemberRepository companyMemberRepository, UserRepository userRepository, RoleRepository roleRepository,
			UserRoleRepository userRoleRepository, PermissionService permissionService, TenantContext tenantContext, AuditService auditService) {
		this.companyMemberRepository = companyMemberRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.permissionService = permissionService;
		this.tenantContext = tenantContext;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public Page<CompanyUserResponse> listUsers(String search, MembershipStatus status, Pageable pageable) {
		permissionService.require("USER_VIEW");
		String normalizedSearch = blankToNull(search);
		Page<CompanyMember> members = loadMembers(normalizedSearch, status, pageable);
		Map<UUID, User> users = userRepository.findByIdIn(members.map(CompanyMember::getUserId).toSet()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		Map<UUID, List<String>> rolesByMembership = members.isEmpty()
				? Map.of()
				: userRoleRepository
						.findRoleNamesByMembershipIds(tenantContext.companyId(), members.map(CompanyMember::getId).toSet())
						.stream()
						.collect(Collectors.groupingBy(
								UserRoleRepository.MembershipRoleName::getMembershipId,
								Collectors.mapping(UserRoleRepository.MembershipRoleName::getRoleName, Collectors.toList())
						));
		return members.map(member -> toResponse(member, users.get(member.getUserId()),
				rolesByMembership.getOrDefault(member.getId(), List.of())));
	}

	private Page<CompanyMember> loadMembers(String search, MembershipStatus status, Pageable pageable) {
		if (search == null && status == null) {
			return companyMemberRepository.findByCompanyIdAndActiveTrueAndMembershipStatusNot(
					tenantContext.companyId(), MembershipStatus.REMOVED, pageable);
		}
		if (search == null) {
			return companyMemberRepository.findByCompanyIdAndActiveTrueAndMembershipStatus(
					tenantContext.companyId(), status, pageable);
		}
		if (status == null) {
			return companyMemberRepository.searchCurrentMembers(tenantContext.companyId(), search, pageable);
		}
		return companyMemberRepository.searchMembers(tenantContext.companyId(), search, status, pageable);
	}

	@Transactional(readOnly = true)
	public CompanyUserResponse getUser(UUID userId) {
		permissionService.require("USER_VIEW");
		CompanyMember member = companyMemberRepository.findByCompanyIdAndUserIdAndActiveTrue(tenantContext.companyId(), userId)
				.orElseThrow(() -> new NotFoundException("COMPANY_USER_NOT_FOUND", "Company user not found"));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
		return toResponse(member, user, userRoleRepository.findRoleNames(userId, tenantContext.companyId()));
	}

	@Transactional
	public CompanyUserResponse assignRoles(UUID userId, AssignRolesRequest request, HttpServletRequest servletRequest) {
		permissionService.require("ROLE_ASSIGN");
		CompanyMember member = companyMemberRepository.findByCompanyIdAndUserIdAndActiveTrue(tenantContext.companyId(), userId)
				.orElseThrow(() -> new NotFoundException("COMPANY_USER_NOT_FOUND", "Company user not found"));
		List<Role> roles = request.roleIds().stream()
				.map(roleId -> roleRepository.findVisibleRole(roleId, tenantContext.companyId())
						.orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found")))
				.toList();
		protectLastOwner(userId, roles.stream().map(Role::getName).toList());
		userRoleRepository.deleteByCompanyIdAndUserId(tenantContext.companyId(), userId);
		for (Role role : roles) {
			UserRole userRole = new UserRole();
			userRole.setCompanyId(tenantContext.companyId());
			userRole.setMembershipId(member.getId());
			userRole.setUserId(userId);
			userRole.setRoleId(role.getId());
			userRole.setCreatedBy(tenantContext.userId());
			userRoleRepository.save(userRole);
		}
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.ROLE_ASSIGNED, "User", userId,
				Map.of("roles", roles.stream().map(Role::getName).toList()), servletRequest);
		return getUser(userId);
	}

	@Transactional
	public CompanyUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, HttpServletRequest servletRequest) {
		permissionService.require("USER_DISABLE");
		CompanyMember member = companyMemberRepository.findByCompanyIdAndUserIdAndActiveTrue(tenantContext.companyId(), userId)
				.orElseThrow(() -> new NotFoundException("COMPANY_USER_NOT_FOUND", "Company user not found"));
		if (request.status() != MembershipStatus.ACTIVE) {
			protectLastOwner(userId, List.of());
		}
		member.setMembershipStatus(request.status());
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.USER_DISABLED, "User", userId,
				Map.of("membershipStatus", request.status().name()), servletRequest);
		return getUser(userId);
	}

	private void protectLastOwner(UUID targetUserId, List<String> newRoleNames) {
		boolean currentlyOwner = userRoleRepository.findRoleNames(targetUserId, tenantContext.companyId()).contains("OWNER");
		boolean willRemainOwner = newRoleNames.contains("OWNER");
		if (currentlyOwner && !willRemainOwner && companyMemberRepository.countActiveOwners(tenantContext.companyId()) <= 1) {
			throw new BusinessRuleException("LAST_OWNER_REQUIRED", "Cannot remove or disable the last active owner");
		}
	}

	private CompanyUserResponse toResponse(CompanyMember member, User user, List<String> roles) {
		return new CompanyUserResponse(user.getId(), member.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
				user.getProfileImageUrl(), user.getStatus().name(), user.getStatus().name(), member.getMembershipStatus().name(),
				member.getJobTitle(), member.getDepartmentName(), member.getJoinedAt(), roles);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
