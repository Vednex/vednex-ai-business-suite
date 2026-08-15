package com.vednex.business_suite.security.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private UUID userId;
	private UUID companyId;
	private UUID membershipId;
	private String tokenHash;
	private UUID familyId;
	private Instant expiresAt;
	private Instant revokedAt;
	private UUID replacedByTokenId;
	private String ipAddress;
	private String userAgent;

	@CreationTimestamp
	private Instant createdAt;

	private Boolean active = true;

	public boolean isUsable(Instant now) {
		return Boolean.TRUE.equals(active) && revokedAt == null && expiresAt.isAfter(now);
	}

}
