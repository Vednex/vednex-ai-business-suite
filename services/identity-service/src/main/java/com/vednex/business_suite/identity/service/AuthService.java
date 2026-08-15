package com.vednex.business_suite.identity.service;

import java.time.Clock;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.vednex.business_suite.company.domain.CompanySettings;
import com.vednex.business_suite.company.domain.CompanyStatus;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.repository.CompanyRepository;
import com.vednex.business_suite.company.repository.CompanySettingsRepository;
import com.vednex.business_suite.events.CorrelationIds;
import com.vednex.business_suite.events.DomainEventPublisher;
import com.vednex.business_suite.events.EventTypes;
import com.vednex.business_suite.identity.domain.EmailVerificationToken;
import com.vednex.business_suite.identity.domain.PasswordResetToken;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.domain.UserStatus;
import com.vednex.business_suite.identity.repository.EmailVerificationTokenRepository;
import com.vednex.business_suite.identity.repository.PasswordResetTokenRepository;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.identity.web.AuthResponse;
import com.vednex.business_suite.identity.web.ForgotPasswordRequest;
import com.vednex.business_suite.identity.web.LoginRequest;
import com.vednex.business_suite.identity.web.LogoutRequest;
import com.vednex.business_suite.identity.web.RefreshTokenRequest;
import com.vednex.business_suite.identity.web.RegisterCompanyRequest;
import com.vednex.business_suite.identity.web.RegistrationResponse;
import com.vednex.business_suite.identity.web.ResendVerificationRequest;
import com.vednex.business_suite.identity.web.ResetPasswordRequest;
import com.vednex.business_suite.identity.web.SwitchCompanyRequest;
import com.vednex.business_suite.identity.web.VerifyEmailRequest;
import com.vednex.business_suite.security.JwtPrincipal;
import com.vednex.business_suite.security.JwtService;
import com.vednex.business_suite.security.PasswordPolicyValidator;
import com.vednex.business_suite.security.TokenService;
import com.vednex.business_suite.security.domain.RefreshToken;
import com.vednex.business_suite.security.domain.Role;
import com.vednex.business_suite.security.domain.UserRole;
import com.vednex.business_suite.security.repository.RefreshTokenRepository;
import com.vednex.business_suite.security.repository.RoleRepository;
import com.vednex.business_suite.security.repository.UserRoleRepository;
import com.vednex.business_suite.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final String OWNER_ROLE = "OWNER";

	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final CompanySettingsRepository companySettingsRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final JwtService jwtService;
	private final TokenService tokenService;
	private final DomainEventPublisher domainEventPublisher;
	private final AuditService auditService;
	private final TenantContext tenantContext;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;
	private final Clock clock;
	private final long accessExpirationMinutes;
	private final long refreshExpirationDays;
	private final long maxFailedLoginAttempts;
	private final long verificationTokenExpirationHours;
	private final long passwordResetTokenExpirationMinutes;
	private final long resendVerificationCooldownMinutes;
	private final String frontendUrl;

	public AuthService(
			UserRepository userRepository,
			CompanyRepository companyRepository,
			CompanySettingsRepository companySettingsRepository,
			CompanyMemberRepository companyMemberRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			RefreshTokenRepository refreshTokenRepository,
			EmailVerificationTokenRepository emailVerificationTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			PasswordEncoder passwordEncoder,
			PasswordPolicyValidator passwordPolicyValidator,
			JwtService jwtService,
			TokenService tokenService,
			DomainEventPublisher domainEventPublisher,
			AuditService auditService,
			TenantContext tenantContext,
			JdbcTemplate jdbcTemplate,
			EntityManager entityManager,
			Clock clock,
			@Value("${app.jwt.access-expiration-minutes}") long accessExpirationMinutes,
			@Value("${app.jwt.refresh-expiration-days}") long refreshExpirationDays,
			@Value("${app.security.max-failed-login-attempts}") long maxFailedLoginAttempts,
			@Value("${app.security.verification-token-expiration-hours}") long verificationTokenExpirationHours,
			@Value("${app.security.password-reset-token-expiration-minutes}") long passwordResetTokenExpirationMinutes,
			@Value("${app.security.resend-verification-cooldown-minutes}") long resendVerificationCooldownMinutes,
			@Value("${app.frontend-url}") String frontendUrl
	) {
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.companySettingsRepository = companySettingsRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.emailVerificationTokenRepository = emailVerificationTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicyValidator = passwordPolicyValidator;
		this.jwtService = jwtService;
		this.tokenService = tokenService;
		this.domainEventPublisher = domainEventPublisher;
		this.auditService = auditService;
		this.tenantContext = tenantContext;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
		this.clock = clock;
		this.accessExpirationMinutes = accessExpirationMinutes;
		this.refreshExpirationDays = refreshExpirationDays;
		this.maxFailedLoginAttempts = maxFailedLoginAttempts;
		this.verificationTokenExpirationHours = verificationTokenExpirationHours;
		this.passwordResetTokenExpirationMinutes = passwordResetTokenExpirationMinutes;
		this.resendVerificationCooldownMinutes = resendVerificationCooldownMinutes;
		this.frontendUrl = frontendUrl;
	}

	@Transactional
	public RegistrationResponse register(RegisterCompanyRequest request, HttpServletRequest servletRequest) {
		passwordPolicyValidator.validate(request.password());
		String email = normalizeEmail(request.email());
		userRepository.findByEmailIgnoreCase(email)
				.ifPresent(existingUser -> {
					throw existingAccountRegistrationException(existingUser);
				});
		String slug = availableSlug(request.companySlug(), request.companyName());
		String companyEmail = request.companyEmail() == null || request.companyEmail().isBlank()
				? email
				: normalizeEmail(request.companyEmail());
		String currency = request.currency() == null || request.currency().isBlank()
				? "USD"
				: request.currency().trim().toUpperCase(Locale.ROOT);

		if (companyRepository.existsByEmailIgnoreCase(companyEmail)) {
			throw new DuplicateResourceException("COMPANY_EMAIL_EXISTS", "A company with this email already exists");
		}

		Instant now = Instant.now(clock);
		Company company = new Company();
		company.setName(request.companyName().trim());
		company.setSlug(slug);
		company.setEmail(companyEmail);
		company.setCountry(request.country().trim());
		company.setTimezone(request.timezone().trim());
		company.setCurrency(currency);
		company.setStatus(CompanyStatus.TRIAL);
		company.setTrialEndsAt(now.plusSeconds(14 * 24 * 60 * 60));
		company = companyRepository.save(company);

		CompanySettings settings = new CompanySettings();
		settings.setCompanyId(company.getId());
		settings.setCountry(company.getCountry());
		settings.setCurrency(company.getCurrency());
		settings.setTimezone(company.getTimezone());
		settings.setLanguage("en");
		settings.setDateFormat("yyyy-MM-dd");
		companySettingsRepository.save(settings);

		User user = new User();
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setEmailVerified(false);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user = userRepository.save(user);

		CompanyMember membership = new CompanyMember();
		membership.setCompanyId(company.getId());
		membership.setUserId(user.getId());
		membership.setMembershipStatus(MembershipStatus.ACTIVE);
		membership.setJoinedAt(now);
		membership = companyMemberRepository.save(membership);

		Role ownerRole = roleRepository.findByCompanyIdIsNullAndName(OWNER_ROLE)
				.orElseThrow(() -> new NotFoundException("OWNER_ROLE_NOT_FOUND", "Owner role is not configured"));
		UserRole userRole = new UserRole();
		userRole.setCompanyId(company.getId());
		userRole.setMembershipId(membership.getId());
		userRole.setUserId(user.getId());
		userRole.setRoleId(ownerRole.getId());
		userRoleRepository.save(userRole);

		entityManager.flush();
		createTrialSubscription(company, user, now);
		publishRegistrationEvents(company, user, membership, servletRequest);
		createVerificationAndQueueEmail(user, servletRequest);
		entityManager.flush();
		auditService.record(company.getId(), user.getId(), AuditAction.COMPANY_REGISTERED, "Company", company.getId(),
				Map.of("slug", company.getSlug()), servletRequest);
		return new RegistrationResponse(user.getId(), company.getId(), user.getEmail(), company.getSlug(), true);
	}

	@Transactional(readOnly = true)
	public RuntimeException registrationConflict(String email) {
		return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
				.<RuntimeException>map(this::existingAccountRegistrationException)
				.orElseGet(() -> new DuplicateResourceException("REGISTRATION_CONFLICT",
						"Registration could not be completed because a matching account or company already exists"));
	}

	@Transactional
	public void verifyEmail(VerifyEmailRequest request, HttpServletRequest servletRequest) {
		EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenService.hashToken(request.token()))
				.orElseThrow(() -> new InvalidTokenException("Verification token is invalid"));
		Instant now = Instant.now(clock);
		if (token.getUsedAt() != null) {
			throw new InvalidTokenException("Verification token has already been used");
		}
		if (!token.getExpiresAt().isAfter(now)) {
			throw new InvalidTokenException("Verification token has expired");
		}
		User user = userRepository.findById(token.getUserId())
				.orElseThrow(() -> new InvalidTokenException("Verification token user is invalid"));
		user.setEmailVerified(true);
		user.setStatus(UserStatus.ACTIVE);
		token.setUsedAt(now);
		companyMemberRepository.findFirstByUserIdAndMembershipStatusAndActiveTrue(user.getId(), MembershipStatus.ACTIVE)
				.ifPresent(membership -> auditService.record(membership.getCompanyId(), user.getId(), AuditAction.EMAIL_VERIFIED,
						"User", user.getId(), Map.of(), servletRequest));
	}

	@Transactional
	public void resendVerification(ResendVerificationRequest request, HttpServletRequest servletRequest) {
		User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
				.orElse(null);
		if (user == null || Boolean.TRUE.equals(user.getEmailVerified())) {
			return;
		}
		Instant now = Instant.now(clock);
		emailVerificationTokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
				.ifPresent(token -> {
					if (token.getLastSentAt() != null
							&& token.getLastSentAt().plusSeconds(resendVerificationCooldownMinutes * 60).isAfter(now)) {
						throw new BusinessRuleException("VERIFICATION_RESEND_COOLDOWN", "Please wait before requesting another verification email");
					}
		});
		emailVerificationTokenRepository.markActiveUnusedTokensUsed(user.getId(), now, now);
		createVerificationAndQueueEmail(user, servletRequest);
	}

	@Transactional
	public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
		User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).orElse(null);
		if (user == null) {
			auditService.record(null, null, AuditAction.LOGIN_FAILED, "User", null, Map.of("reason", "unknown_email"), servletRequest);
			throw invalidCredentials();
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			registerFailedLogin(user, servletRequest);
			throw invalidCredentials();
		}
		if (!Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new BadRequestException("EMAIL_NOT_VERIFIED", "Please verify your email before logging in");
		}
		if (user.getStatus() != UserStatus.ACTIVE || Boolean.TRUE.equals(user.getAccountLocked())) {
			throw new BadRequestException("ACCOUNT_NOT_ACTIVE", "The user account is not active");
		}

		CompanyMember membership = companyMemberRepository
				.findFirstByUserIdAndMembershipStatusAndActiveTrue(user.getId(), MembershipStatus.ACTIVE)
				.orElseThrow(() -> new BadRequestException("NO_ACTIVE_COMPANY", "The user has no active company membership"));
		Company company = companyRepository.findById(membership.getCompanyId())
				.orElseThrow(() -> new NotFoundException("COMPANY_NOT_FOUND", "Company not found"));

		user.setFailedLoginAttempts(0);
		user.setAccountLocked(false);
		user.setLastLoginAt(Instant.now(clock));
		AuthResponse response = issueTokens(user, company, membership, servletRequest, null);
		auditService.record(company.getId(), user.getId(), AuditAction.LOGIN_SUCCESS, "User", user.getId(), Map.of(), servletRequest);
		return response;
	}

	@Transactional(noRollbackFor = InvalidTokenException.class)
	public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
		RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenService.hashToken(request.refreshToken()))
				.orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));
		Instant now = Instant.now(clock);
		if (!currentToken.getExpiresAt().isAfter(now)) {
			throw new InvalidTokenException("Refresh token has expired");
		}
		if (!Boolean.TRUE.equals(currentToken.getActive()) || currentToken.getRevokedAt() != null) {
			revokeRefreshFamilyForReuse(currentToken, now, servletRequest);
			throw new InvalidTokenException("Refresh token has expired or was revoked");
		}

		User user = userRepository.findById(currentToken.getUserId())
				.orElseThrow(() -> rejectRefreshAndRevokeFamily(currentToken, now,
						"ACCOUNT_NOT_ACTIVE", "The user account is not active"));
		validateRefreshUser(user, currentToken, now);

		CompanyMember membership = companyMemberRepository.findById(currentToken.getMembershipId())
				.orElseThrow(() -> rejectRefreshAndRevokeFamily(currentToken, now,
						"MEMBERSHIP_NOT_ACTIVE", "The company membership is not active"));
		validateRefreshMembership(membership, currentToken, now);

		Company company = companyRepository.findById(currentToken.getCompanyId())
				.orElseThrow(() -> rejectRefreshAndRevokeFamily(currentToken, now,
						"COMPANY_NOT_ACTIVE", "The company is not active"));
		validateRefreshCompany(company, currentToken, now);

		currentToken.setRevokedAt(now);
		currentToken.setActive(false);
		AuthResponse response = issueTokens(user, company, membership, servletRequest, currentToken.getFamilyId());
		currentToken.setReplacedByTokenId(refreshTokenRepository.findByTokenHash(tokenService.hashToken(response.refreshToken()))
				.map(RefreshToken::getId)
				.orElse(null));
		return response;
	}

	private void validateRefreshUser(User user, RefreshToken currentToken, Instant now) {
		if (user.getStatus() == UserStatus.LOCKED || Boolean.TRUE.equals(user.getAccountLocked())) {
			throw rejectRefreshAndRevokeFamily(currentToken, now,
					"ACCOUNT_LOCKED", "The user account is locked");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw rejectRefreshAndRevokeFamily(currentToken, now,
					"ACCOUNT_NOT_ACTIVE", "The user account is not active");
		}
		if (!Boolean.TRUE.equals(user.getEmailVerified())) {
			throw rejectRefreshAndRevokeFamily(currentToken, now,
					"EMAIL_NOT_VERIFIED", "The user account is not active");
		}
	}

	private void validateRefreshMembership(CompanyMember membership, RefreshToken currentToken, Instant now) {
		if (!Objects.equals(membership.getId(), currentToken.getMembershipId())
				|| !Objects.equals(membership.getUserId(), currentToken.getUserId())
				|| !Objects.equals(membership.getCompanyId(), currentToken.getCompanyId())
				|| membership.getMembershipStatus() != MembershipStatus.ACTIVE
				|| !Boolean.TRUE.equals(membership.getActive())) {
			throw rejectRefreshAndRevokeFamily(currentToken, now,
					"MEMBERSHIP_NOT_ACTIVE", "The company membership is not active");
		}
	}

	private void validateRefreshCompany(Company company, RefreshToken currentToken, Instant now) {
		if (!Objects.equals(company.getId(), currentToken.getCompanyId())
				|| (company.getStatus() != CompanyStatus.TRIAL && company.getStatus() != CompanyStatus.ACTIVE)) {
			throw rejectRefreshAndRevokeFamily(currentToken, now,
					"COMPANY_NOT_ACTIVE", "The company is not active");
		}
	}

	private InvalidTokenException rejectRefreshAndRevokeFamily(RefreshToken currentToken, Instant now, String code, String message) {
		refreshTokenRepository.revokeFamily(currentToken.getFamilyId(), now);
		return new InvalidTokenException(code, message);
	}

	private void revokeRefreshFamilyForReuse(RefreshToken currentToken, Instant now, HttpServletRequest servletRequest) {
		refreshTokenRepository.revokeFamily(currentToken.getFamilyId(), now);
		auditService.record(currentToken.getCompanyId(), currentToken.getUserId(), AuditAction.REFRESH_TOKEN_REUSED,
				"RefreshToken", currentToken.getId(), Map.of(), servletRequest);
	}

	@Transactional
	public void logout(LogoutRequest request, HttpServletRequest servletRequest) {
		refreshTokenRepository.findByTokenHash(tokenService.hashToken(request.refreshToken()))
				.ifPresent(token -> {
					token.setRevokedAt(Instant.now(clock));
					token.setActive(false);
					auditService.record(token.getCompanyId(), token.getUserId(), AuditAction.LOGOUT, "RefreshToken", token.getId(),
							Map.of(), servletRequest);
				});
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest servletRequest) {
		User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).orElse(null);
		if (user == null || user.getStatus() == UserStatus.DISABLED) {
			return;
		}
		String rawToken = tokenService.createOpaqueToken();
		Instant expiresAt = Instant.now(clock).plusSeconds(passwordResetTokenExpirationMinutes * 60);
		PasswordResetToken token = new PasswordResetToken();
		token.setUserId(user.getId());
		token.setTokenHash(tokenService.hashToken(rawToken));
		token.setExpiresAt(expiresAt);
		passwordResetTokenRepository.save(token);
		publishPasswordResetEmailRequested(user, rawToken, expiresAt, servletRequest);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request, HttpServletRequest servletRequest) {
		passwordPolicyValidator.validate(request.newPassword());
		PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenService.hashToken(request.token()))
				.orElseThrow(() -> new InvalidTokenException("Password reset token is invalid"));
		Instant now = Instant.now(clock);
		if (token.getUsedAt() != null) {
			throw new InvalidTokenException("Password reset token has already been used");
		}
		if (!token.getExpiresAt().isAfter(now)) {
			throw new InvalidTokenException("Password reset token has expired");
		}
		User user = userRepository.findById(token.getUserId())
				.orElseThrow(() -> new InvalidTokenException("Password reset token user is invalid"));
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setFailedLoginAttempts(0);
		user.setAccountLocked(false);
		user.setStatus(UserStatus.ACTIVE);
		token.setUsedAt(now);
		refreshTokenRepository.revokeActiveUserTokens(user.getId(), now);
		auditService.record(null, user.getId(), AuditAction.PASSWORD_RESET, "User", user.getId(), Map.of(), servletRequest);
	}

	@Transactional
	public AuthResponse switchCompany(SwitchCompanyRequest request, HttpServletRequest servletRequest) {
		UUID userId = tenantContext.userId();
		CompanyMember membership = companyMemberRepository
				.findByCompanyIdAndUserIdAndActiveTrue(request.companyId(), userId)
				.filter(member -> member.getMembershipStatus() == MembershipStatus.ACTIVE)
				.orElseThrow(() -> new BadRequestException("INVALID_COMPANY_MEMBERSHIP", "You are not an active member of this company"));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
		Company company = companyRepository.findById(request.companyId())
				.orElseThrow(() -> new NotFoundException("COMPANY_NOT_FOUND", "Company not found"));
		AuthResponse response = issueTokens(user, company, membership, servletRequest, null);
		auditService.record(company.getId(), user.getId(), AuditAction.TENANT_SWITCHED, "Company", company.getId(), Map.of(), servletRequest);
		return response;
	}

	private AuthResponse issueTokens(User user, Company company, CompanyMember membership, HttpServletRequest servletRequest, UUID existingFamilyId) {
		List<String> roles = userRoleRepository.findRoleNames(user.getId(), company.getId());
		List<String> permissions = userRoleRepository.findPermissionCodes(user.getId(), company.getId());
		String rawRefreshToken = tokenService.createOpaqueToken();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUserId(user.getId());
		refreshToken.setCompanyId(company.getId());
		refreshToken.setMembershipId(membership.getId());
		refreshToken.setTokenHash(tokenService.hashToken(rawRefreshToken));
		refreshToken.setFamilyId(existingFamilyId == null ? UUID.randomUUID() : existingFamilyId);
		refreshToken.setExpiresAt(Instant.now(clock).plusSeconds(refreshExpirationDays * 24 * 60 * 60));
		refreshToken.setIpAddress(clientIp(servletRequest));
		refreshToken.setUserAgent(userAgent(servletRequest));
		refreshTokenRepository.save(refreshToken);

		JwtPrincipal principal = new JwtPrincipal(user.getId(), user.getEmail(), company.getId(), membership.getId(), roles, permissions);
		return AuthResponse.bearer(
				jwtService.createAccessToken(principal),
				rawRefreshToken,
				accessExpirationMinutes * 60,
				new AuthResponse.AuthenticatedUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
						Boolean.TRUE.equals(user.getEmailVerified())),
				new AuthResponse.AuthenticatedCompanyResponse(company.getId(), company.getName(), company.getSlug(), company.getStatus().name()),
				roles,
				permissions
		);
	}

	private void createVerificationAndQueueEmail(User user, HttpServletRequest servletRequest) {
		String rawToken = tokenService.createOpaqueToken();
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plusSeconds(verificationTokenExpirationHours * 60 * 60);
		EmailVerificationToken token = new EmailVerificationToken();
		token.setUserId(user.getId());
		token.setTokenHash(tokenService.hashToken(rawToken));
		token.setExpiresAt(expiresAt);
		token.setLastSentAt(now);
		emailVerificationTokenRepository.save(token);
		publishVerificationEmailRequested(user, rawToken, expiresAt, servletRequest);
	}

	private void publishVerificationEmailRequested(User user, String rawToken, Instant expiresAt, HttpServletRequest servletRequest) {
		Optional<CompanyMember> membership = companyMemberRepository
				.findFirstByUserIdAndMembershipStatusAndActiveTrue(user.getId(), MembershipStatus.ACTIVE);
		UUID companyId = membership.map(CompanyMember::getCompanyId).orElse(null);
		Map<String, Object> data = new HashMap<>();
		data.put("recipientEmail", user.getEmail());
		data.put("recipientName", displayName(user));
		data.put("verificationUrl", frontendUrl + "/verify-email?token=" + rawToken);
		data.put("expiresAt", expiresAt.toString());
		domainEventPublisher.publish(
				"User",
				user.getId(),
				EventTypes.IDENTITY_EMAIL_VERIFICATION_REQUESTED_V1,
				1,
				companyId,
				user.getId(),
				CorrelationIds.from(servletRequest),
				data
		);
	}

	private void publishPasswordResetEmailRequested(User user, String rawToken, Instant expiresAt, HttpServletRequest servletRequest) {
		Map<String, Object> data = new HashMap<>();
		data.put("recipientEmail", user.getEmail());
		data.put("recipientName", displayName(user));
		data.put("resetUrl", frontendUrl + "/reset-password?token=" + rawToken);
		data.put("expiresAt", expiresAt.toString());
		domainEventPublisher.publish(
				"User",
				user.getId(),
				EventTypes.IDENTITY_PASSWORD_RESET_REQUESTED_V1,
				1,
				null,
				user.getId(),
				CorrelationIds.from(servletRequest),
				data
		);
	}

	private void publishRegistrationEvents(Company company, User user, CompanyMember membership, HttpServletRequest servletRequest) {
		UUID correlationId = CorrelationIds.from(servletRequest);
		domainEventPublisher.publish(
				"Company",
				company.getId(),
				EventTypes.IDENTITY_COMPANY_CREATED_V1,
				1,
				company.getId(),
				user.getId(),
				correlationId,
				Map.of(
						"companyId", company.getId().toString(),
						"companySlug", company.getSlug(),
						"status", company.getStatus().name(),
						"country", company.getCountry(),
						"currency", company.getCurrency(),
						"timezone", company.getTimezone()
				)
		);
		domainEventPublisher.publish(
				"User",
				user.getId(),
				EventTypes.IDENTITY_USER_REGISTERED_V1,
				1,
				company.getId(),
				user.getId(),
				correlationId,
				Map.of(
						"userId", user.getId().toString(),
						"companyId", company.getId().toString(),
						"membershipId", membership.getId().toString(),
						"email", user.getEmail(),
						"emailVerified", Boolean.TRUE.equals(user.getEmailVerified()),
						"status", user.getStatus().name()
				)
		);
	}

	private RuntimeException existingAccountRegistrationException(User user) {
		if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DISABLED || Boolean.TRUE.equals(user.getAccountLocked())) {
			return new BusinessRuleException("ACCOUNT_NOT_ACTIVE", "An account already exists with this email. Please contact support.");
		}
		if (!Boolean.TRUE.equals(user.getEmailVerified()) || user.getStatus() == UserStatus.PENDING_VERIFICATION) {
			return new DuplicateResourceException("EMAIL_VERIFICATION_PENDING",
					"Your account is already registered but your email has not been verified.");
		}
		if (user.getStatus() == UserStatus.ACTIVE) {
			return new DuplicateResourceException("ACCOUNT_ALREADY_EXISTS", "An account already exists with this email. Please log in.");
		}
		return new DuplicateResourceException("ACCOUNT_ALREADY_EXISTS", "An account already exists with this email. Please log in.");
	}

	private void registerFailedLogin(User user, HttpServletRequest servletRequest) {
		int attempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
		user.setFailedLoginAttempts(attempts);
		if (attempts >= maxFailedLoginAttempts) {
			user.setAccountLocked(true);
			user.setStatus(UserStatus.LOCKED);
		}
		auditService.record(null, user.getId(), AuditAction.LOGIN_FAILED, "User", user.getId(), Map.of("reason", "invalid_password"), servletRequest);
	}

	private void createTrialSubscription(Company company, User user, Instant now) {
		UUID planId = jdbcTemplate.queryForObject("select id from plans where code = 'TRIAL'", UUID.class);
		jdbcTemplate.update("""
				insert into subscriptions (company_id, plan_id, status, starts_at, trial_ends_at, created_by)
				values (?, ?, 'TRIAL', ?, ?, ?)
				""", company.getId(), planId, Timestamp.from(now), Timestamp.from(company.getTrialEndsAt()), user.getId());
	}

	private String availableSlug(String requestedSlug, String companyName) {
		String baseSlug = requestedSlug == null || requestedSlug.isBlank() ? slugify(companyName) : requestedSlug.trim();
		String slug = baseSlug;
		int suffix = 2;
		while (companyRepository.existsBySlug(slug)) {
			slug = baseSlug + "-" + suffix;
			suffix++;
		}
		return slug;
	}

	private String slugify(String value) {
		String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return slug.isBlank() ? "company" : slug;
	}

	private BadRequestException invalidCredentials() {
		return new BadRequestException("INVALID_CREDENTIALS", "Email or password is incorrect");
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

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String userAgent(HttpServletRequest request) {
		String userAgent = request.getHeader("User-Agent");
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
	}

}
