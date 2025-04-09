package com.spring2025.vietchefs.services;

/**
 * Service interface for managing roles
 */
public interface RoleService {
    /**
     * Gets role name by role ID
     * 
     * @param roleId The ID of the role
     * @return The name of the role
     */
    String getRoleNameById(Long roleId);
} 