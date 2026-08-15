package com.vednex.business_suite.company.web;

import java.util.UUID;

import com.vednex.business_suite.common.exception.BadRequestException;
import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.service.CompanyUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/users")
public class CompanyUserController {

	private final CompanyUserService companyUserService;

	public CompanyUserController(CompanyUserService companyUserService) {
		this.companyUserService = companyUserService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<CompanyUserResponse>>> listUsers(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) MembershipStatus status,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@PageableDefault(size = 20) Pageable pageable
	) {
		if (page != null && page < 0) {
			throw new BadRequestException("INVALID_PAGE", "Page index must be zero or greater");
		}
		if (size != null && size < 1) {
			throw new BadRequestException("INVALID_PAGE_SIZE", "Page size must be greater than zero");
		}
		return ResponseEntity.ok(ApiResponse.success("Company users loaded", companyUserService.listUsers(search, status, pageable)));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<ApiResponse<CompanyUserResponse>> getUser(@PathVariable UUID userId) {
		return ResponseEntity.ok(ApiResponse.success("Company user loaded", companyUserService.getUser(userId)));
	}

	@PutMapping("/{userId}/roles")
	public ResponseEntity<ApiResponse<CompanyUserResponse>> assignRoles(
			@PathVariable UUID userId,
			@Valid @RequestBody AssignRolesRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Roles assigned", companyUserService.assignRoles(userId, request, servletRequest)));
	}

	@PutMapping("/{userId}/status")
	public ResponseEntity<ApiResponse<CompanyUserResponse>> updateStatus(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateUserStatusRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("User status updated", companyUserService.updateStatus(userId, request, servletRequest)));
	}

}
