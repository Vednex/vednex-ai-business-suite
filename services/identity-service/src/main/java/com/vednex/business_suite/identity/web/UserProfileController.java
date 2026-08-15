package com.vednex.business_suite.identity.web;

import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.identity.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
		return ResponseEntity.ok(ApiResponse.success("User profile loaded", userProfileService.me()));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(@Valid @RequestBody UpdateMeRequest request) {
		return ResponseEntity.ok(ApiResponse.success("User profile updated", userProfileService.updateMe(request)));
	}

	@PutMapping("/password")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		userProfileService.changePassword(request);
		return ResponseEntity.ok(ApiResponse.success("Password updated", null));
	}

}
