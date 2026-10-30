package com.weatherwise.controller;

import com.weatherwise.dto.ApiResponse;
import com.weatherwise.dto.RouteAnalysisResponse;
import com.weatherwise.dto.RouteRequest;
import com.weatherwise.dto.RouteResponse;
import com.weatherwise.service.RouteService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/routes")
public class RoutePlannerController {

    private final RouteService routeService;

    public RoutePlannerController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/plan")
    public ResponseEntity<ApiResponse<RouteAnalysisResponse>> planRoute(
            @Valid @RequestBody RouteRequest request
    ) {

        RouteAnalysisResponse routeAnalysis =
                routeService.planRoute(request);

        ApiResponse<RouteAnalysisResponse> response =
                new ApiResponse<>(
                        true,
                        "Route planned successfully",
                        routeAnalysis
                );

        return ResponseEntity
                .ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RouteResponse>>> getAllRoutes(
            Pageable pageable
    ) {

        Page<RouteResponse> routes =
                routeService.getAllRoutes(pageable);

        ApiResponse<Page<RouteResponse>> response =
                new ApiResponse<>(
                        true,
                        "Routes fetched successfully",
                        routes
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<Page<RouteResponse>>> getSavedRoutes(
            Pageable pageable
    ) {

        Page<RouteResponse> routes =
                routeService.getSavedRoutes(pageable);

        ApiResponse<Page<RouteResponse>> response =
                new ApiResponse<>(
                        true,
                        "Saved routes fetched successfully",
                        routes
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteById(
            @PathVariable Long id
    ) {

        RouteResponse route =
                routeService.getRouteById(id);

        ApiResponse<RouteResponse> response =
                new ApiResponse<>(
                        true,
                        "Route fetched successfully",
                        route
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<ApiResponse<RouteResponse>> saveRoute(
            @PathVariable Long id
    ) {

        RouteResponse route =
                routeService.saveRoute(id);

        ApiResponse<RouteResponse> response =
                new ApiResponse<>(
                        true,
                        "Route saved successfully",
                        route
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<ApiResponse<RouteResponse>> unsaveRoute(
            @PathVariable Long id
    ) {

        RouteResponse route =
                routeService.unsaveRoute(id);

        ApiResponse<RouteResponse> response =
                new ApiResponse<>(
                        true,
                        "Route removed from saved routes",
                        route
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearHistory() {

        routeService.clearHistory();

        Map<String, String> data = Map.of(
                "status",
                "CLEARED"
        );

        ApiResponse<Map<String, String>> response =
                new ApiResponse<>(
                        true,
                        "Unsaved route history cleared successfully",
                        data
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteRoute(
            @PathVariable Long id
    ) {

        routeService.deleteRoute(id);

        Map<String, String> data = Map.of(
                "routeId",
                id.toString(),
                "status",
                "DELETED"
        );

        ApiResponse<Map<String, String>> response =
                new ApiResponse<>(
                        true,
                        "Route deleted successfully",
                        data
                );

        return ResponseEntity.ok(response);
    }
}