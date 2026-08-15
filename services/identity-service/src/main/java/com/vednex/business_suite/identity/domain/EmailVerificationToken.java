package com.vednex.business_suite.identity.domain;

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
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private UUID userId;
	private String tokenHash;
	private Instant expiresAt;
	private Instant usedAt;
	private Instant lastSentAt;

	@CreationTimestamp
	private Instant createdAt;

}
