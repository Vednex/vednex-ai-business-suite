package com.vednex.business_suite.company.web;

import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.company.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

	private final CompanyService companyService;

	public CompanyController(CompanyService companyService) {
		this.companyService = companyService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<CompanyResponse>> getCompany() {
		return ResponseEntity.ok(ApiResponse.success("Company loaded", companyService.getCompany()));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
			@Valid @RequestBody UpdateCompanyRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Company updated", companyService.updateCompany(request, servletRequest)));
	}

	@GetMapping("/settings")
	public ResponseEntity<ApiResponse<CompanySettingsResponse>> getSettings() {
		return ResponseEntity.ok(ApiResponse.success("Company settings loaded", companyService.getSettings()));
	}

	@PutMapping("/settings")
	public ResponseEntity<ApiResponse<CompanySettingsResponse>> updateSettings(
			@Valid @RequestBody UpdateCompanySettingsRequest request,
			HttpServletRequest servletRequest
	) {
		return ResponseEntity.ok(ApiResponse.success("Company settings updated", companyService.updateSettings(request, servletRequest)));
	}

	@GetMapping("/dashboard-summary")
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> dashboardSummary() {
		return ResponseEntity.ok(ApiResponse.success("Dashboard summary loaded", companyService.dashboardSummary()));
	}

}
