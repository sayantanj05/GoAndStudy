package com.goandstudybackend.service;

import com.goandstudybackend.entity.Role;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Map<String, Object> createRole(String name, String description, String adminId) {
        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("Role with name '" + name + "' already exists");
        }

        Role role = roleRepository.save(Role.builder()
                .name(name)
                .description(description)
                .createdByAdminId(adminId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        return Map.of(
                "roleId", role.getId(),
                "name", role.getName(),
                "description", role.getDescription(),
                "createdAt", role.getCreatedAt()
        );
    }

    public Map<String, Object> getAllRoles(int page, int limit) {
        List<Role> roles = roleRepository.findAll(PageRequest.of(Math.max(page - 1, 0), limit)).getContent();
        long total = roleRepository.count();
        return Map.of("roles", roles, "total", total, "page", page, "pages", (int) Math.ceil((double) total / limit));
    }

    public Role getRoleById(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    public Map<String, Object> updateRole(String roleId, String name, String description, String adminId) {
        Role role = getRoleById(roleId);

        if (!role.getName().equals(name) && roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("Role with name '" + name + "' already exists");
        }

        role.setName(name);
        role.setDescription(description);
        role.setUpdatedAt(LocalDateTime.now());

        Role updatedRole = roleRepository.save(role);

        return Map.of(
                "roleId", updatedRole.getId(),
                "name", updatedRole.getName(),
                "description", updatedRole.getDescription(),
                "updatedAt", updatedRole.getUpdatedAt()
        );
    }

    public Map<String, Object> deleteRole(String roleId, String adminId) {
        Role role = getRoleById(roleId);
        roleRepository.delete(role);
        return Map.of("roleId", roleId, "deleted", true);
    }
}
