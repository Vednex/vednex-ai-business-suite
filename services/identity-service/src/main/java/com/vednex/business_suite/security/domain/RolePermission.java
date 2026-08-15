package com.vednex.business_suite.security.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(RolePermissionId.class)
@Table(name = "role_permissions")
public class RolePermission {

	@Id
	private UUID roleId;

	@Id
	private UUID permissionId;

}
