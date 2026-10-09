package com.weatherwise.controller;

import com.weatherwise.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {

        ApiResponse<String> response =
                new ApiResponse<>(
                        true,
                        "Backend is running successfully",
                        "Roamora Backend"
                );

        return ResponseEntity.ok(response);
    }
}