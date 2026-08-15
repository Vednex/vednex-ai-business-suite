package com.vednex.business_suite.identity.web;

import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.identity.service.AuthService;
import com.vednex.business_suite.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegistrationResponse>> register(
			@Valid @RequestBody RegisterCompanyRequest request,
			HttpServletRequest servletRequest
	) {
		try {
			return ResponseEntity.ok(ApiResponse.success("Registration successful. Please verify your email.",
					authService.register(request, servletRequest)));
		}
		catch (DataIntegrityViolationException ex) {
			throw authService.registrationConflict(request.email());
		}
	}

	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<Void>> verifyEmail(
			@Valid @RequestBody VerifyEmailRequest request,
			HttpServletRequest servletRequest
	) {
		authService.verifyEmail(request, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("Email verified", null));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<ApiResponse<Void>> resendVerification(
			@Valid @RequestBody ResendVerificationRequest request,
			HttpServletRequest servletRequest
	) {
		authService.resendVerification(request, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("If your account requires verification, a verification email has been sent.", null));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request, servletRequest)));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refresh(request, servletRequest)));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest servletRequest) {
		authService.logout(request, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest request,
			HttpServletRequest servletRequest
	) {
		authService.forgotPassword(request, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("Password reset email queued if the account exists", null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(
			@Valid @RequestBody ResetPasswordRequest request,
			HttpServletRequest servletRequest
	) {
		authService.resetPassword(request, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
	}

	@PostMapping("/switch-company")
	public ResponseEntity<ApiResponse<AuthResponse>> switchCompany(
			@Valid @RequestBody SwitchCompanyRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Company switched", authService.switchCompany(request, servletRequest)));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal JwtPrincipal principal) {
		return ResponseEntity.ok(ApiResponse.success(
				"Authenticated user loaded",
				new MeResponse(
						principal.userId(),
						principal.email(),
						principal.companyId(),
						principal.membershipId(),
						principal.roles(),
						principal.permissions()
				)
		));
	}

}
