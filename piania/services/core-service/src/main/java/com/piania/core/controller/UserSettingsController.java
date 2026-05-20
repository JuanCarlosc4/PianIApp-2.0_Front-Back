package com.piania.core.controller;

import com.piania.core.dto.usersettings.UserSettingsRequest;
import com.piania.core.dto.usersettings.UserSettingsResponse;
import com.piania.core.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService service;

    @GetMapping
    public ResponseEntity<UserSettingsResponse> get(Authentication auth) {
        return ResponseEntity.ok(service.getByUserEmail(auth.getName()));
    }

    @PutMapping
    public ResponseEntity<UserSettingsResponse> upsert(
            Authentication auth,
            @Valid @RequestBody UserSettingsRequest request) {
        return ResponseEntity.ok(service.upsert(auth.getName(), request));
    }
}
