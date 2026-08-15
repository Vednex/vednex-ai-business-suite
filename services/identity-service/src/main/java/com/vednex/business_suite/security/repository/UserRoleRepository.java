package com.vednex.business_suite.security.repository;

import java.util.List;
import java.util.UUID;

import com.vednex.business_suite.security.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	@Query("""
			select r.name
			from UserRole ur
			join Role r on r.id = ur.roleId
			where ur.companyId = :companyId
			  and ur.userId = :userId
			  and ur.active = true
			  and r.active = true
			order by r.name
			""")
	List<String> findRoleNames(@Param("userId") UUID userId, @Param("companyId") UUID companyId);

	@Query("""
			select distinct p.code
			from UserRole ur
			join RolePermission rp on rp.roleId = ur.roleId
			join Permission p on p.id = rp.permissionId
			where ur.companyId = :companyId
			  and ur.userId = :userId
			  and ur.active = true
			  and p.active = true
			order by p.code
			""")
	List<String> findPermissionCodes(@Param("userId") UUID userId, @Param("companyId") UUID companyId);

	@Query("""
			select ur.membershipId as membershipId, r.name as roleName
			from UserRole ur
			join Role r on r.id = ur.roleId
			where ur.companyId = :companyId
			  and ur.membershipId in :membershipIds
			  and ur.active = true
			  and r.active = true
			order by r.name
			""")
	List<MembershipRoleName> findRoleNamesByMembershipIds(
			@Param("companyId") UUID companyId,
			@Param("membershipIds") java.util.Collection<UUID> membershipIds
	);

	List<UserRole> findByCompanyIdAndUserIdAndActiveTrue(UUID companyId, UUID userId);

	boolean existsByRoleIdAndActiveTrue(UUID roleId);

	void deleteByCompanyIdAndUserId(UUID companyId, UUID userId);

	interface MembershipRoleName {
		UUID getMembershipId();

		String getRoleName();
	}

}
