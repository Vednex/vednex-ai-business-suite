package com.vednex.business_suite.identity.domain;

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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String firstName;
	private String lastName;
	private String email;
	private String passwordHash;
	private String phone;
	private String profileImageUrl;
	private Boolean emailVerified = false;
	private Boolean accountLocked = false;
	private Integer failedLoginAttempts = 0;
	private Instant lastLoginAt;

	@Enumerated(EnumType.STRING)
	private UserStatus status;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	@Version
	private Long version;

}
