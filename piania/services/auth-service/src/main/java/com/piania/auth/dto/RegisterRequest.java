package com.piania.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String fullName,
        String accountType
) {}
