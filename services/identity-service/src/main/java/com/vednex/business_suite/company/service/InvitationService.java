package com.vednex.business_suite.company.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.vednex.business_suite.audit.AuditAction;
import com.vednex.business_suite.audit.AuditService;
import com.vednex.business_suite.common.exception.BadRequestException;
import com.vednex.business_suite.common.exception.BusinessRuleException;
import com.vednex.business_suite.common.exception.DuplicateResourceException;
import com.vednex.business_suite.common.exception.InvalidTokenException;
import com.vednex.business_suite.common.exception.NotFoundException;
import com.vednex.business_suite.company.domain.Company;
import com.vednex.business_suite.company.domain.CompanyMember;
import com.vednex.business_suite.company.domain.Invitation;
import com.vednex.business_suite.company.domain.InvitationStatus;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.repository.CompanyRepository;
import com.vednex.business_suite.company.repository.InvitationRepository;
import com.vednex.business_suite.company.web.AcceptInvitationRequest;
import com.vednex.business_suite.company.web.CreateInvitationRequest;
import com.vednex.business_suite.company.web.InvitationResponse;
import com.vednex.business_suite.events.CorrelationIds;
import com.vednex.business_suite.events.DomainEventPublisher;
import com.vednex.business_suite.events.EventTypes;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.domain.UserStatus;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.security.PasswordPolicyValidator;
import com.vednex.business_suite.security.PermissionService;
import com.vednex.business_suite.security.TokenService;
import com.vednex.business_suite.security.domain.Role;
import com.vednex.business_suite.security.domain.UserRole;
import com.vednex.business_suite.security.repository.RoleRepository;
import com.vednex.business_suite.security.repository.UserRoleRepository;
import com.vednex.business_suite.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

	private final InvitationRepository invitationRepository;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final CompanyRepository companyRepository;
	private final UserRoleRepository userRoleRepository;
	private final PermissionService permissionService;
	private final TenantContext tenantContext;
	private final TokenService tokenService;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final DomainEventPublisher domainEventPublisher;
	private final AuditService auditService;
	private final Clock clock;
	private final long invitationExpirationDays;
	private final String frontendUrl;

	public InvitationService(InvitationRepository invitationRepository, RoleRepository roleRepository, UserRepository userRepository,
			CompanyMemberRepository companyMemberRepository, CompanyRepository companyRepository,
			UserRoleRepository userRoleRepository, PermissionService permissionService,
			TenantContext tenantContext, TokenService tokenService, PasswordEncoder passwordEncoder, PasswordPolicyValidator passwordPolicyValidator,
			DomainEventPublisher domainEventPublisher, AuditService auditService, Clock clock,
			@Value("${app.security.invitation-expiration-days}") long invitationExpirationDays,
			@Value("${app.frontend-url}") String frontendUrl) {
		this.invitationRepository = invitationRepository;
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.companyRepository = companyRepository;
		this.userRoleRepository = userRoleRepository;
		this.permissionService = permissionService;
		this.tenantContext = tenantContext;
		this.tokenService = tokenService;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicyValidator = passwordPolicyValidator;
		this.domainEventPublisher = domainEventPublisher;
		this.auditService = auditService;
		this.clock = clock;
		this.invitationExpirationDays = invitationExpirationDays;
		this.frontendUrl = frontendUrl;
	}

	@Transactional
	public InvitationResponse create(CreateInvitationRequest request, HttpServletRequest servletRequest) {
		permissionService.require("USER_INVITE");
		String email = normalizeEmail(request.email());
		Role role = roleRepository.findVisibleRole(request.roleId(), tenantContext.companyId())
				.orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
		if (invitationRepository.existsActiveInvitation(tenantContext.companyId(), email, InvitationStatus.PENDING)) {
			throw new DuplicateResourceException("INVITATION_EXISTS", "There is already a pending invitation for this email");
		}
		if (companyMemberRepository.findByCompanyIdAndUserIdAndActiveTrue(tenantContext.companyId(),
				userRepository.findByEmailIgnoreCase(email).map(User::getId).orElse(UUID.randomUUID())).isPresent()) {
			throw new BusinessRuleException("USER_ALREADY_MEMBER", "This user is already a company member");
		}
		String rawToken = tokenService.createOpaqueToken();
		Invitation invitation = new Invitation();
		invitation.setCompanyId(tenantContext.companyId());
		invitation.setEmail(email);
		invitation.setRoleId(role.getId());
		invitation.setTokenHash(tokenService.hashToken(rawToken));
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now(clock).plusSeconds(invitationExpirationDays * 24 * 60 * 60));
		invitation.setInvitedBy(tenantContext.userId());
		invitation.setCreatedBy(tenantContext.userId());
		invitation = invitationRepository.save(invitation);
		publishInvitationEmailRequested(invitation, role, rawToken, servletRequest);
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.USER_INVITED, "Invitation", invitation.getId(),
				Map.of("email", email, "role", role.getName()), servletRequest);
		return toResponse(invitation);
	}

	@Transactional(readOnly = true)
	public Page<InvitationResponse> list(Pageable pageable) {
		permissionService.require("USER_INVITE");
		return invitationRepository.findByCompanyIdAndActiveTrue(tenantContext.companyId(), pageable).map(this::toResponse);
	}

	@Transactional
	public InvitationResponse accept(String rawToken, AcceptInvitationRequest request, HttpServletRequest servletRequest) {
		Invitation invitation = invitationRepository.findByTokenHash(tokenService.hashToken(rawToken))
				.orElseThrow(() -> new InvalidTokenException("Invitation token is invalid"));
		Instant now = Instant.now(clock);
		if (invitation.getStatus() != InvitationStatus.PENDING || invitation.getAcceptedAt() != null || invitation.getRevokedAt() != null) {
			throw new InvalidTokenException("Invitation token has already been used");
		}
		if (!invitation.getExpiresAt().isAfter(now)) {
			invitation.setStatus(InvitationStatus.EXPIRED);
			throw new InvalidTokenException("Invitation token has expired");
		}
		User user = userRepository.findByEmailIgnoreCase(invitation.getEmail()).orElseGet(() -> createInvitedUser(invitation, request));
		if (companyMemberRepository.existsByCompanyIdAndUserIdAndActiveTrue(invitation.getCompanyId(), user.getId())) {
			throw new BusinessRuleException("USER_ALREADY_MEMBER", "This user is already a company member");
		}
		CompanyMember member = new CompanyMember();
		member.setCompanyId(invitation.getCompanyId());
		member.setUserId(user.getId());
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		member.setJoinedAt(now);
		member = companyMemberRepository.save(member);

		UserRole userRole = new UserRole();
		userRole.setCompanyId(invitation.getCompanyId());
		userRole.setMembershipId(member.getId());
		userRole.setUserId(user.getId());
		userRole.setRoleId(invitation.getRoleId());
		userRole.setCreatedBy(invitation.getInvitedBy());
		userRoleRepository.save(userRole);

		invitation.setAcceptedAt(now);
		invitation.setStatus(InvitationStatus.ACCEPTED);
		userRepository.flush();
		auditService.record(invitation.getCompanyId(), user.getId(), AuditAction.INVITATION_ACCEPTED, "Invitation", invitation.getId(),
				Map.of(), servletRequest);
		return toResponse(invitation);
	}

	@Transactional
	public InvitationResponse resend(UUID id, HttpServletRequest servletRequest) {
		permissionService.require("USER_INVITE");
		Invitation invitation = loadTenantInvitation(id);
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new BusinessRuleException("INVITATION_NOT_PENDING", "Only pending invitations can be resent");
		}
		String rawToken = tokenService.createOpaqueToken();
		invitation.setTokenHash(tokenService.hashToken(rawToken));
		invitation.setExpiresAt(Instant.now(clock).plusSeconds(invitationExpirationDays * 24 * 60 * 60));
		invitation.setUpdatedBy(tenantContext.userId());
		Role role = roleRepository.findVisibleRole(invitation.getRoleId(), invitation.getCompanyId())
				.orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
		publishInvitationEmailRequested(invitation, role, rawToken, servletRequest);
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.USER_INVITED, "Invitation", invitation.getId(),
				Map.of("resent", true), servletRequest);
		return toResponse(invitation);
	}

	@Transactional
	public void revoke(UUID id, HttpServletRequest servletRequest) {
		permissionService.require("USER_INVITE");
		Invitation invitation = loadTenantInvitation(id);
		invitation.setRevokedAt(Instant.now(clock));
		invitation.setStatus(InvitationStatus.REVOKED);
		invitation.setActive(false);
		invitation.setUpdatedBy(tenantContext.userId());
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.USER_INVITED, "Invitation", invitation.getId(),
				Map.of("revoked", true), servletRequest);
	}

	private Invitation loadTenantInvitation(UUID id) {
		return invitationRepository.findByIdAndCompanyIdAndActiveTrue(id, tenantContext.companyId())
				.orElseThrow(() -> new NotFoundException("INVITATION_NOT_FOUND", "Invitation not found"));
	}

	private User createInvitedUser(Invitation invitation, AcceptInvitationRequest request) {
		if (request.password() == null || request.firstName() == null || request.lastName() == null) {
			throw new BadRequestException("ACCOUNT_DETAILS_REQUIRED", "First name, last name, and password are required");
		}
		passwordPolicyValidator.validate(request.password());
		User user = new User();
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(invitation.getEmail());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setEmailVerified(true);
		user.setStatus(UserStatus.ACTIVE);
		return userRepository.save(user);
	}

	private InvitationResponse toResponse(Invitation invitation) {
		return new InvitationResponse(invitation.getId(), invitation.getEmail(), invitation.getRoleId(), invitation.getStatus().name(),
				invitation.getExpiresAt(), invitation.getAcceptedAt(), invitation.getRevokedAt());
	}

	private void publishInvitationEmailRequested(Invitation invitation, Role role, String rawToken, HttpServletRequest servletRequest) {
		Company company = companyRepository.findById(invitation.getCompanyId())
				.orElseThrow(() -> new NotFoundException("COMPANY_NOT_FOUND", "Company not found"));
		String inviterName = userRepository.findById(invitation.getInvitedBy()).map(this::displayName).orElse("Vednex user");
		Map<String, Object> data = new HashMap<>();
		data.put("recipientEmail", invitation.getEmail());
		data.put("inviterName", inviterName);
		data.put("companyName", company.getName());
		data.put("roleName", role.getName());
		data.put("invitationUrl", frontendUrl + "/invitations/accept?token=" + rawToken);
		data.put("expiresAt", invitation.getExpiresAt().toString());
		domainEventPublisher.publish(
				"Invitation",
				invitation.getId(),
				EventTypes.IDENTITY_USER_INVITED_V1,
				1,
				invitation.getCompanyId(),
				invitation.getInvitedBy(),
				CorrelationIds.from(servletRequest),
				data
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String displayName(User user) {
		String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
		String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
		String fullName = (firstName + " " + lastName).trim();
		return fullName.isBlank() ? user.getEmail() : fullName;
	}

}
