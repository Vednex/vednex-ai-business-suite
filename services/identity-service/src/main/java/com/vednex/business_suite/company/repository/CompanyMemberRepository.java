package com.vednex.business_suite.company.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.company.domain.CompanyMember;
import com.vednex.business_suite.company.domain.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {

	Optional<CompanyMember> findFirstByUserIdAndMembershipStatusAndActiveTrue(UUID userId, MembershipStatus status);

	List<CompanyMember> findByUserIdAndActiveTrue(UUID userId);

	boolean existsByCompanyIdAndUserIdAndActiveTrue(UUID companyId, UUID userId);

	Optional<CompanyMember> findByCompanyIdAndUserIdAndActiveTrue(UUID companyId, UUID userId);

	Optional<CompanyMember> findByIdAndCompanyIdAndActiveTrue(UUID id, UUID companyId);

	@Query("""
			select count(cm)
			from CompanyMember cm
			join UserRole ur on ur.membershipId = cm.id
			join Role r on r.id = ur.roleId
			where cm.companyId = :companyId
			  and cm.membershipStatus = com.vednex.business_suite.company.domain.MembershipStatus.ACTIVE
			  and cm.active = true
			  and ur.active = true
			  and r.name = 'OWNER'
			""")
	long countActiveOwners(@Param("companyId") UUID companyId);

	long countByCompanyIdAndMembershipStatusAndActiveTrue(UUID companyId, MembershipStatus membershipStatus);

	Page<CompanyMember> findByCompanyIdAndActiveTrueAndMembershipStatusNot(
			UUID companyId,
			MembershipStatus membershipStatus,
			Pageable pageable
	);

	Page<CompanyMember> findByCompanyIdAndActiveTrueAndMembershipStatus(
			UUID companyId,
			MembershipStatus membershipStatus,
			Pageable pageable
	);

	@Query("""
			select cm
			from CompanyMember cm
			join User u on u.id = cm.userId
			where cm.companyId = :companyId
			  and cm.active = true
			  and cm.membershipStatus = :status
			  and (lower(u.email) like lower(concat('%', :search, '%'))
			       or lower(u.firstName) like lower(concat('%', :search, '%'))
			       or lower(u.lastName) like lower(concat('%', :search, '%'))
			       or lower(concat(concat(u.firstName, ' '), u.lastName)) like lower(concat('%', :search, '%')))
			""")
	Page<CompanyMember> searchMembers(
			@Param("companyId") UUID companyId,
			@Param("search") String search,
			@Param("status") MembershipStatus status,
			Pageable pageable
	);

	@Query("""
			select cm
			from CompanyMember cm
			join User u on u.id = cm.userId
			where cm.companyId = :companyId
			  and cm.active = true
			  and cm.membershipStatus <> com.vednex.business_suite.company.domain.MembershipStatus.REMOVED
			  and (lower(u.email) like lower(concat('%', :search, '%'))
			       or lower(u.firstName) like lower(concat('%', :search, '%'))
			       or lower(u.lastName) like lower(concat('%', :search, '%'))
			       or lower(concat(concat(u.firstName, ' '), u.lastName)) like lower(concat('%', :search, '%')))
			""")
	Page<CompanyMember> searchCurrentMembers(
			@Param("companyId") UUID companyId,
			@Param("search") String search,
			Pageable pageable
	);

}
