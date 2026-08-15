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
@Table(name = "permissions")
public class Permission {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String code;
	private String description;

	@CreationTimestamp
	private Instant createdAt;

	private Boolean active = true;

}
