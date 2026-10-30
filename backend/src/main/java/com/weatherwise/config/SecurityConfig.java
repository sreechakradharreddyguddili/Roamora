package com.weatherwise.config;

import com.weatherwise.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> {})

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        // Authentication
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        )
                        .permitAll()

                        // OAuth2 temporarily disabled
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**"
                        )
                        .permitAll()

                        // Health
                        .requestMatchers(
                                "/api/v1/health"
                        )
                        .permitAll()

                        // Public weather/location APIs
                        .requestMatchers(
                                "/api/v1/weather/**",
                                "/api/v1/locations/**"
                        )
                        .permitAll()

                        // Public nearby destination discovery.
                        .requestMatchers(
                                "/api/v1/places/**"
                        )
                        .permitAll()

                        // Actuator
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        )
                        .permitAll()

                        // Public transport-place APIs
                        .requestMatchers(
                                "/api/v1/transport-places/**"
                        )
                        .permitAll()

                        // Public GTFS feed lookup
                        .requestMatchers(
                                "/api/v1/gtfs/feeds"
                        )
                        .permitAll()

                        // Public read-only global transport directory; admin routes remain protected.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/global-transport-places",
                                "/api/v1/global-transport-places/nearby"
                        )
                        .permitAll()

                        // GTFS ingestion is an administrative operation.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/gtfs/ingest"
                        )
                        .hasRole("ADMIN")

                        // Public read-only GTFS stop lookup and realtime snapshots.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/gtfs/stops",
                                "/api/v1/gtfs/departures",
                                "/api/v1/gtfs/realtime/vehicles",
                                "/api/v1/gtfs/realtime/trip-updates",
                                "/api/v1/gtfs/realtime/alerts"
                        )
                        .permitAll()

                        // GTFS schedule lookup
                        .requestMatchers(
                                "/api/v1/gtfs/schedules"
                        )
                        .permitAll()

                        // GTFS departure lookup
                        .requestMatchers(
                                "/api/v1/gtfs/departures"
                        )
                        .permitAll()

                        // GTFS calendar debug endpoint
                        .requestMatchers(
                                "/api/v1/gtfs/calendar-debug"
                        )
                        .permitAll()

                        // GTFS calendar count endpoint
                        .requestMatchers(
                                "/api/v1/gtfs/calendar-count"
                        )
                        .permitAll()

                        // GTFS feed administration.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/gtfs/feeds/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/gtfs/feeds/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/gtfs/feeds/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/gtfs/feeds/**"
                        )
                        .hasRole("ADMIN")

                        // GTFS realtime refresh is administrative.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/gtfs/realtime/refresh/**"
                        )
                        .hasRole("ADMIN")

                        // Global transport imports are administrative.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/global-transport/admin/**"
                        )
                        .hasRole("ADMIN")

                        // Route planning itself can be used without an account.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/routes/plan"
                        )
                        .permitAll()

                        // Route history and saved journeys are private.
                        .requestMatchers(
                                "/api/v1/routes/**"
                        )
                        .authenticated()

                        // Public transport journey search
                        .requestMatchers(
                                "/api/v1/transport/journeys/**"
                        )
                        .permitAll()

                        // Profile requires login
                        .requestMatchers(
                                "/api/v1/profile"
                        )
                        .authenticated()

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                // JWT authentication
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}