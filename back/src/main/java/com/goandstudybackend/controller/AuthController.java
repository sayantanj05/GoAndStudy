package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.LoginRequest;
import com.goandstudybackend.dto.request.RegisterRequest;
import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.dto.response.AuthResponse;
import com.goandstudybackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login as admin, staff, or member")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        System.out.println("LOGIN DEBUG - Email: " + request.getEmail() + ", Role: " + response.getRole());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Register a new member")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        Map<String, Object> data = Map.of(
                "memberId", authResponse.getUserId(),
                "name", authResponse.getName(),
                "token", authResponse.getToken(),
                "role", authResponse.getRole(),
                "expiresIn", authResponse.getExpiresIn()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registration successful", data));
    }

    @Operation(summary = "Logout current user")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(Authentication authentication) {
        if (authentication != null) {
            authService.logout(authentication.getName(), authentication.getAuthorities().stream().findFirst().map(Object::toString).orElse(""));
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", Map.of("message", "Logged out successfully")));
    }
}
