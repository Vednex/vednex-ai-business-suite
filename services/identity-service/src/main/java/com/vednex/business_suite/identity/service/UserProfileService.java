package com.vednex.business_suite.identity.service;

import java.time.Clock;
import java.time.Instant;

import com.vednex.business_suite.common.exception.BadRequestException;
import com.vednex.business_suite.common.exception.NotFoundException;
import com.vednex.business_suite.identity.domain.User;
import com.vednex.business_suite.identity.repository.UserRepository;
import com.vednex.business_suite.identity.web.ChangePasswordRequest;
import com.vednex.business_suite.identity.web.UpdateMeRequest;
import com.vednex.business_suite.identity.web.UserProfileResponse;
import com.vednex.business_suite.security.PasswordPolicyValidator;
import com.vednex.business_suite.security.repository.RefreshTokenRepository;
import com.vednex.business_suite.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

	private final UserRepository userRepository;
	private final TenantContext tenantContext;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final RefreshTokenRepository refreshTokenRepository;
	private final Clock clock;

	public UserProfileService(UserRepository userRepository, TenantContext tenantContext, PasswordEncoder passwordEncoder,
			PasswordPolicyValidator passwordPolicyValidator, RefreshTokenRepository refreshTokenRepository, Clock clock) {
		this.userRepository = userRepository;
		this.tenantContext = tenantContext;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicyValidator = passwordPolicyValidator;
		this.refreshTokenRepository = refreshTokenRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse me() {
		return toResponse(loadUser());
	}

	@Transactional
	public UserProfileResponse updateMe(UpdateMeRequest request) {
		User user = loadUser();
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(blankToNull(request.phone()));
		user.setProfileImageUrl(blankToNull(request.profileImageUrl()));
		return toResponse(user);
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		User user = loadUser();
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadRequestException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
		}
		passwordPolicyValidator.validate(request.newPassword());
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeActiveUserTokens(user.getId(), Instant.now(clock));
	}

	private User loadUser() {
		return userRepository.findById(tenantContext.userId())
				.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
	}

	private UserProfileResponse toResponse(User user) {
		return new UserProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone(),
				user.getProfileImageUrl(), Boolean.TRUE.equals(user.getEmailVerified()), user.getStatus().name());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
