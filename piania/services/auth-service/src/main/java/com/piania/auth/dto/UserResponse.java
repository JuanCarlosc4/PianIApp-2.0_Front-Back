package com.piania.auth.dto;

import com.piania.auth.entity.Role;

public record UserResponse(
        String id,
        String email,
        String fullName,
        String avatar,
        Role role,
        boolean premium,
        boolean chatNotificationsEnabled
) {}
