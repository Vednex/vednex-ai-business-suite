package com.vednex.business_suite.security.repository;

import java.util.List;
import java.util.UUID;

import com.vednex.business_suite.security.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	List<Permission> findByActiveTrueOrderByCode();

	List<Permission> findByCodeInAndActiveTrue(java.util.Collection<String> codes);

}
