package com.vednex.business_suite.company.repository;

import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.company.domain.Invitation;
import com.vednex.business_suite.company.domain.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

	Optional<Invitation> findByTokenHash(String tokenHash);

	Optional<Invitation> findByIdAndCompanyIdAndActiveTrue(UUID id, UUID companyId);

	Page<Invitation> findByCompanyIdAndActiveTrue(UUID companyId, Pageable pageable);

	@Query("""
			select count(i) > 0
			from Invitation i
			where i.companyId = :companyId
			  and lower(i.email) = lower(:email)
			  and i.status = :status
			  and i.active = true
			""")
	boolean existsActiveInvitation(
			@Param("companyId") UUID companyId,
			@Param("email") String email,
			@Param("status") InvitationStatus status
	);

	long countByCompanyIdAndStatusAndActiveTrue(UUID companyId, InvitationStatus status);

}
