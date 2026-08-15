package com.vednex.business_suite.security.repository;

import java.util.Optional;
import java.util.UUID;

import com.vednex.business_suite.security.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

	Optional<Role> findByCompanyIdIsNullAndName(String name);

	Optional<Role> findByIdAndActiveTrue(UUID id);

	@Query("""
			select r
			from Role r
			where r.active = true
			  and (r.companyId = :companyId or r.companyId is null)
			order by r.protectedSystemRole desc, r.name
			""")
	java.util.List<Role> findVisibleRoles(@Param("companyId") UUID companyId);

	@Query("""
			select r
			from Role r
			where r.id = :roleId
			  and r.active = true
			  and (r.companyId = :companyId or r.companyId is null)
			""")
	Optional<Role> findVisibleRole(@Param("roleId") UUID roleId, @Param("companyId") UUID companyId);

}
