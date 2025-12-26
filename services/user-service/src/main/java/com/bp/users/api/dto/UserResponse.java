package com.bp.users.api.dto;

public record UserResponse(
        String id,
        String email,
        String fullName
) {}