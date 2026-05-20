package com.piania.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.piania.auth.dto.AuthResponse;
import com.piania.auth.dto.LoginRequest;
import com.piania.auth.dto.RegisterRequest;
import com.piania.auth.dto.UserResponse;
import com.piania.auth.service.AuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/piania/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            // Avoid Spring's "Required request body is missing" 500 and return a clean 400
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    @PutMapping("/me/avatar/{avatar}")
    public ResponseEntity<UserResponse> updateAvatar(
            Authentication authentication,
            @PathVariable String avatar
    ) {
        return ResponseEntity.ok(authService.updateAvatar(authentication.getName(), avatar));
    }
}
