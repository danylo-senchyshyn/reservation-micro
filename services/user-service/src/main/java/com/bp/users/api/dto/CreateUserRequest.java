package com.bp.users.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The type Create user request.
 */
public record CreateUserRequest(
        @NotBlank String email,
        @NotBlank String fullName
) {}