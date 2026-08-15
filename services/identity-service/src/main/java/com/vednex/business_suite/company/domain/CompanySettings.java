package com.vednex.business_suite.company.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "company_settings")
public class CompanySettings {

	@Id
	private UUID companyId;

	private String logoUrl;

	@Column(name = "address_line_1")
	private String addressLine1;

	@Column(name = "address_line_2")
	private String addressLine2;

	private String city;
	private String state;
	private String postalCode;
	private String country;
	private String gstNumber;
	private String taxNumber;
	private String currency;
	private String timezone;
	private String language;
	private String dateFormat;

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
