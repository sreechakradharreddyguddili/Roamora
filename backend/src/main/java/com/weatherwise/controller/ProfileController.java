package com.weatherwise.controller;

import com.weatherwise.auth.User;
import com.weatherwise.dto.ApiResponse;
import com.weatherwise.dto.ProfileResponse;
import com.weatherwise.security.CurrentUserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(
            CurrentUserService currentUserService
    ) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {

        User user =
                currentUserService.getCurrentUser();

        ProfileResponse profile =
                new ProfileResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt()
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Profile fetched successfully",
                        profile
                )
        );
    }
}