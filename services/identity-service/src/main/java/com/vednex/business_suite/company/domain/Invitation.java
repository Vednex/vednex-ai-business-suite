package com.vednex.business_suite.company.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "invitations")
public class Invitation {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private UUID companyId;
	private String email;
	private UUID roleId;
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	private InvitationStatus status;

	private Instant expiresAt;
	private Instant acceptedAt;
	private Instant revokedAt;
	private UUID invitedBy;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	private UUID createdBy;
	private UUID updatedBy;

	@Version
	private Long version;

	private Boolean active = true;

}
