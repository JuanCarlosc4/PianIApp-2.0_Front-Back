package com.piania.auth.service;

import com.piania.auth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    UserResponse me(String email);

    UserResponse updateAvatar(String email, String avatar);
}
