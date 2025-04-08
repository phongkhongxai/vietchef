package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the RoleService interface
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Gets role name by role ID
     * 
     * @param roleId The ID of the role
     * @return The name of the role
     * @throws ResourceNotFoundException if the role with the given ID is not found
     */
    @Override
    public String getRoleNameById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        
        return role.getRoleName();
    }
} 