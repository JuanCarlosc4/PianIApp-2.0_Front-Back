package com.piania.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
