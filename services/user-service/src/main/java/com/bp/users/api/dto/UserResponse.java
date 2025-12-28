package com.bp.users.api.dto;

/**
 * The type User response.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName
) {}