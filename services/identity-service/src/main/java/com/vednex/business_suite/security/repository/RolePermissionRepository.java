package com.vednex.business_suite.security.repository;

import java.util.List;
import java.util.UUID;

import com.vednex.business_suite.security.domain.RolePermission;
import com.vednex.business_suite.security.domain.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

	void deleteByRoleId(UUID roleId);

	List<RolePermission> findByRoleId(UUID roleId);

	@org.springframework.data.jpa.repository.Query("""
			select p.code
			from RolePermission rp
			join Permission p on p.id = rp.permissionId
			where rp.roleId = :roleId
			  and p.active = true
			order by p.code
			""")
	List<String> findPermissionCodes(@org.springframework.data.repository.query.Param("roleId") UUID roleId);

}
