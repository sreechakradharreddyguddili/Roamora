package com.weatherwise.auth;

import com.weatherwise.dto.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AuthResponse response =
                authService.register(request);

        return new ApiResponse<>(
                true,
                "Registration successful",
                response
        );
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return new ApiResponse<>(
                true,
                "Login successful",
                response
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @RequestBody Map<String, String> request
    ) {

        String refreshToken =
                request.get("refreshToken");

        if (refreshToken == null ||
                refreshToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token is required"
            );
        }

        AuthResponse response =
                authService.refresh(refreshToken);

        return new ApiResponse<>(
                true,
                "Token refreshed successfully",
                response
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(
            @RequestBody Map<String, String> request
    ) {

        String refreshToken =
                request.get("refreshToken");

        if (refreshToken != null &&
                !refreshToken.isBlank()) {

            authService.logout(refreshToken);
        }

        return new ApiResponse<>(
                true,
                "Logout successful",
                Map.of(
                        "status",
                        "LOGGED_OUT"
                )
        );
    }
}