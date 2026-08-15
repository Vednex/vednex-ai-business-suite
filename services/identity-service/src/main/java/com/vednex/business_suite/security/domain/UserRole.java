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
@Table(name = "user_roles")
public class UserRole {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private UUID companyId;
	private UUID membershipId;
	private UUID userId;
	private UUID roleId;

	@CreationTimestamp
	private Instant createdAt;

	private UUID createdBy;
	private Boolean active = true;

}
