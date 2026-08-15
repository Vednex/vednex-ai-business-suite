package com.vednex.business_suite.security.web;

import java.util.List;
import java.util.UUID;

import com.vednex.business_suite.common.model.ApiResponse;
import com.vednex.business_suite.security.service.RoleManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

	private final RoleManagementService roleManagementService;

	public RoleController(RoleManagementService roleManagementService) {
		this.roleManagementService = roleManagementService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
		return ResponseEntity.ok(ApiResponse.success("Roles loaded", roleManagementService.listRoles()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Role created", roleManagementService.create(request)));
	}

	@GetMapping("/{roleId}")
	public ResponseEntity<ApiResponse<RoleResponse>> get(@PathVariable UUID roleId) {
		return ResponseEntity.ok(ApiResponse.success("Role loaded", roleManagementService.get(roleId)));
	}

	@PutMapping("/{roleId}")
	public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable UUID roleId, @Valid @RequestBody RoleRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Role updated", roleManagementService.update(roleId, request)));
	}

	@DeleteMapping("/{roleId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID roleId) {
		roleManagementService.delete(roleId);
		return ResponseEntity.ok(ApiResponse.success("Role deleted", null));
	}

	@GetMapping("/permissions")
	public ResponseEntity<ApiResponse<List<PermissionResponse>>> permissions() {
		return ResponseEntity.ok(ApiResponse.success("Permissions loaded", roleManagementService.listPermissions()));
	}

}
