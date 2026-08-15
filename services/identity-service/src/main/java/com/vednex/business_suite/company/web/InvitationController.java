package com.vednex.business_suite.company.web;

import java.util.UUID;

import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.company.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/invitations")
public class InvitationController {

	private final InvitationService invitationService;

	public InvitationController(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<InvitationResponse>> create(
			@Valid @RequestBody CreateInvitationRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Invitation created", invitationService.create(request, servletRequest)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<InvitationResponse>>> list(@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Invitations loaded", invitationService.list(pageable)));
	}

	@PostMapping("/{token}/accept")
	public ResponseEntity<ApiResponse<InvitationResponse>> accept(
			@PathVariable String token,
			@Valid @RequestBody AcceptInvitationRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Invitation accepted", invitationService.accept(token, request, servletRequest)));
	}

	@PostMapping("/{id}/resend")
	public ResponseEntity<ApiResponse<InvitationResponse>> resend(@PathVariable UUID id, HttpServletRequest servletRequest) {
		return ResponseEntity.ok(ApiResponse.success("Invitation resent", invitationService.resend(id, servletRequest)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id, HttpServletRequest servletRequest) {
		invitationService.revoke(id, servletRequest);
		return ResponseEntity.ok(ApiResponse.success("Invitation revoked", null));
	}

}
