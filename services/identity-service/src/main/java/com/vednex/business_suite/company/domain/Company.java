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
@Table(name = "companies")
public class Company {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String name;
	private String legalName;
	private String slug;
	private String email;
	private String phone;
	private String country;
	private String timezone;
	private String currency;

	@Enumerated(EnumType.STRING)
	private CompanyStatus status;

	private Instant trialEndsAt;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	@Version
	private Long version;

}
