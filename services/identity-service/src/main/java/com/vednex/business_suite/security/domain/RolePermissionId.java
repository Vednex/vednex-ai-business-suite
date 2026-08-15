package com.vednex.business_suite.security.domain;

import java.io.Serializable;
import java.util.UUID;

public record RolePermissionId(UUID roleId, UUID permissionId) implements Serializable {
}
