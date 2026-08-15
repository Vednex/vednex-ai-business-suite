package com.vednex.business_suite.company.repository;

import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

	Optional<Company> findBySlug(String slug);

	@Query("select count(c) > 0 from Company c where lower(c.email) = lower(:email)")
	boolean existsByEmailIgnoreCase(@Param("email") String email);

	boolean existsBySlug(String slug);

	Optional<Company> findByIdAndStatusNot(UUID id, com.vednex.business_suite.company.domain.CompanyStatus status);

}
