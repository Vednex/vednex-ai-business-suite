package com.vednex.business_suite.audit;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public AuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void record(UUID companyId, UUID userId, AuditAction action, String entityType, UUID entityId,
			Map<String, Object> metadata, HttpServletRequest request) {
		try {
			String metadataJson = objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
			jdbcTemplate.update("""
					insert into audit_logs (company_id, user_id, action, entity_type, entity_id, metadata, ip_address, user_agent)
					values (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
					""", companyId, userId, action.name(), entityType, entityId, metadataJson, clientIp(request), userAgent(request));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to record audit event", ex);
		}
	}

	private String clientIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String userAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String userAgent = request.getHeader("User-Agent");
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
	}

}
