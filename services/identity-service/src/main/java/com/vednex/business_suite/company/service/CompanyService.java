package com.vednex.business_suite.company.service;

import java.util.Map;
import java.util.UUID;

import com.vednex.business_suite.audit.AuditAction;
import com.vednex.business_suite.audit.AuditService;
import com.vednex.business_suite.common.exception.NotFoundException;
import com.vednex.business_suite.company.domain.Company;
import com.vednex.business_suite.company.domain.CompanySettings;
import com.vednex.business_suite.company.domain.InvitationStatus;
import com.vednex.business_suite.company.domain.MembershipStatus;
import com.vednex.business_suite.company.repository.CompanyMemberRepository;
import com.vednex.business_suite.company.repository.CompanyRepository;
import com.vednex.business_suite.company.repository.CompanySettingsRepository;
import com.vednex.business_suite.company.repository.InvitationRepository;
import com.vednex.business_suite.company.web.CompanyResponse;
import com.vednex.business_suite.company.web.CompanySettingsResponse;
import com.vednex.business_suite.company.web.DashboardSummaryResponse;
import com.vednex.business_suite.company.web.UpdateCompanyRequest;
import com.vednex.business_suite.company.web.UpdateCompanySettingsRequest;
import com.vednex.business_suite.security.PermissionService;
import com.vednex.business_suite.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

	private final CompanyRepository companyRepository;
	private final CompanySettingsRepository companySettingsRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final InvitationRepository invitationRepository;
	private final PermissionService permissionService;
	private final TenantContext tenantContext;
	private final AuditService auditService;
	private final JdbcTemplate jdbcTemplate;

	public CompanyService(CompanyRepository companyRepository, CompanySettingsRepository companySettingsRepository,
			CompanyMemberRepository companyMemberRepository, InvitationRepository invitationRepository,
			PermissionService permissionService, TenantContext tenantContext, AuditService auditService, JdbcTemplate jdbcTemplate) {
		this.companyRepository = companyRepository;
		this.companySettingsRepository = companySettingsRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.invitationRepository = invitationRepository;
		this.permissionService = permissionService;
		this.tenantContext = tenantContext;
		this.auditService = auditService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public CompanyResponse getCompany() {
		permissionService.require("COMPANY_VIEW");
		return toCompanyResponse(loadCompany());
	}

	@Transactional
	public CompanyResponse updateCompany(UpdateCompanyRequest request, HttpServletRequest servletRequest) {
		permissionService.require("COMPANY_UPDATE");
		Company company = loadCompany();
		company.setName(request.name().trim());
		company.setLegalName(blankToNull(request.legalName()));
		company.setEmail(request.email().trim().toLowerCase());
		company.setPhone(blankToNull(request.phone()));
		company.setCountry(request.country().trim());
		company.setTimezone(request.timezone().trim());
		company.setCurrency(request.currency().trim().toUpperCase());
		auditService.record(company.getId(), tenantContext.userId(), AuditAction.COMPANY_UPDATED, "Company", company.getId(), Map.of(), servletRequest);
		return toCompanyResponse(company);
	}

	@Transactional(readOnly = true)
	public CompanySettingsResponse getSettings() {
		permissionService.require("COMPANY_VIEW");
		return toSettingsResponse(loadSettings());
	}

	@Transactional
	public CompanySettingsResponse updateSettings(UpdateCompanySettingsRequest request, HttpServletRequest servletRequest) {
		permissionService.require("COMPANY_UPDATE");
		CompanySettings settings = loadSettings();
		settings.setLogoUrl(blankToNull(request.logoUrl()));
		settings.setAddressLine1(blankToNull(request.addressLine1()));
		settings.setAddressLine2(blankToNull(request.addressLine2()));
		settings.setCity(blankToNull(request.city()));
		settings.setState(blankToNull(request.state()));
		settings.setPostalCode(blankToNull(request.postalCode()));
		settings.setCountry(blankToNull(request.country()));
		settings.setGstNumber(blankToNull(request.gstNumber()));
		settings.setTaxNumber(blankToNull(request.taxNumber()));
		settings.setCurrency(defaultString(request.currency(), settings.getCurrency()).toUpperCase());
		settings.setTimezone(defaultString(request.timezone(), settings.getTimezone()));
		settings.setLanguage(defaultString(request.language(), settings.getLanguage()));
		settings.setDateFormat(defaultString(request.dateFormat(), settings.getDateFormat()));
		settings.setUpdatedBy(tenantContext.userId());
		auditService.record(tenantContext.companyId(), tenantContext.userId(), AuditAction.COMPANY_UPDATED, "CompanySettings",
				tenantContext.companyId(), Map.of(), servletRequest);
		return toSettingsResponse(settings);
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse dashboardSummary() {
		Company company = loadCompany();
		String plan = jdbcTemplate.queryForObject("""
				select p.name
				from subscriptions s
				join plans p on p.id = s.plan_id
				where s.company_id = ? and s.active = true
				order by s.created_at desc
				limit 1
				""", String.class, company.getId());
		long totalUsers = companyMemberRepository.countByCompanyIdAndMembershipStatusAndActiveTrue(company.getId(), MembershipStatus.ACTIVE);
		long pendingInvitations = invitationRepository.countByCompanyIdAndStatusAndActiveTrue(company.getId(), InvitationStatus.PENDING);
		return new DashboardSummaryResponse(company.getName(), plan, company.getTrialEndsAt(), totalUsers, totalUsers, pendingInvitations);
	}

	private Company loadCompany() {
		UUID companyId = tenantContext.companyId();
		return companyRepository.findById(companyId)
				.orElseThrow(() -> new NotFoundException("COMPANY_NOT_FOUND", "Company not found"));
	}

	private CompanySettings loadSettings() {
		return companySettingsRepository.findById(tenantContext.companyId())
				.orElseThrow(() -> new NotFoundException("COMPANY_SETTINGS_NOT_FOUND", "Company settings not found"));
	}

	private CompanyResponse toCompanyResponse(Company company) {
		return new CompanyResponse(company.getId(), company.getName(), company.getLegalName(), company.getSlug(), company.getEmail(),
				company.getPhone(), company.getCountry(), company.getTimezone(), company.getCurrency(), company.getStatus().name(),
				company.getTrialEndsAt());
	}

	private CompanySettingsResponse toSettingsResponse(CompanySettings settings) {
		return new CompanySettingsResponse(settings.getLogoUrl(), settings.getAddressLine1(), settings.getAddressLine2(), settings.getCity(),
				settings.getState(), settings.getPostalCode(), settings.getCountry(), settings.getGstNumber(), settings.getTaxNumber(),
				settings.getCurrency(), settings.getTimezone(), settings.getLanguage(), settings.getDateFormat());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String defaultString(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

}
