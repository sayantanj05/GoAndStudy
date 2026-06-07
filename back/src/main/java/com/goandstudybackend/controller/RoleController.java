package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.CreateRoleRequest;
import com.goandstudybackend.dto.request.UpdateRoleRequest;
import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all roles")
    public ResponseEntity<ApiResponse<Object>> getAllRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success("Roles fetched", roleService.getAllRoles(page, limit)));
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<Object>> getRole(@PathVariable String roleId) {
        return ResponseEntity.ok(ApiResponse.success("Role fetched", roleService.getRoleById(roleId)));
    }

    @PostMapping
    @Operation(summary = "Create new role")
    public ResponseEntity<ApiResponse<Object>> createRole(
            Authentication authentication,
            @Valid @RequestBody CreateRoleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created", roleService.createRole(
                        request.getName(),
                        request.getDescription(),
                        authentication.getName()
                )));
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "Update role")
    public ResponseEntity<ApiResponse<Object>> updateRole(
            Authentication authentication,
            @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Role updated", roleService.updateRole(
                roleId,
                request.getName(),
                request.getDescription(),
                authentication.getName()
        )));
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete role")
    public ResponseEntity<ApiResponse<Object>> deleteRole(
            Authentication authentication,
            @PathVariable String roleId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Role deleted", roleService.deleteRole(roleId, authentication.getName())));
    }
}
