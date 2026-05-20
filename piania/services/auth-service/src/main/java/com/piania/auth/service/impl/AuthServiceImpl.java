package com.piania.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.piania.auth.dto.*;
import com.piania.auth.entity.*;
import com.piania.auth.exception.CustomException;
import com.piania.auth.repository.*;
import com.piania.auth.security.JwtService;
import com.piania.auth.service.*;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new CustomException(409, "Email already registered");
        }

        Role role = Role.USER;
        if (request.accountType() != null) {
            String type = request.accountType().trim().toUpperCase();
            if (type.equals("TEACHER") || type.equals("PROFESOR") || type.equals("PROFESSOR")) {
                role = Role.TEACHER;
            } else if (type.equals("ADMIN")) {
                role = Role.ADMIN;
            }
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName() != null ? request.fullName().trim() : request.email())
                .avatar(User.Avatar.AVATAR_1)
                .role(role)
                .premium(false)
                .chatNotificationsEnabled(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(404, "User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(401, "Invalid credentials");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new CustomException(401, "Invalid refresh token"));

        String newAccessToken = jwtService.generateToken(token.getUser());

        return new AuthResponse(newAccessToken, refreshToken);
    }

    @Override
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(404, "User not found"));

        String avatar = user.getAvatar() != null ? user.getAvatar().name() : User.Avatar.AVATAR_1.name();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                Objects.requireNonNullElse(user.getFullName(), user.getEmail()),
                avatar,
                user.getRole(),
                user.isPremium(),
                user.isChatNotificationsEnabled()
        );
    }

    @Override
    public UserResponse updateAvatar(String email, String avatar) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(404, "User not found"));

        User.Avatar newAvatar;
        try {
            newAvatar = User.Avatar.valueOf(avatar.trim().toUpperCase());
        } catch (Exception e) {
            throw new CustomException(400, "Invalid avatar");
        }

        user.setAvatar(newAvatar);
        userRepository.save(user);

        return me(email);
    }
}
