package com.weatherwise.oauth2;

import com.weatherwise.auth.User;
import com.weatherwise.auth.UserRepository;
import com.weatherwise.security.JwtService;
import com.weatherwise.security.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${app.oauth2.authorized-redirect-uri}")
            String redirectUri
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendRedirect(
                    redirectUri + "?error=" +
                            encode("Email not available")
            );
            return;
        }

        email = email.trim().toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OAuth2 user was not created"
                                )
                        );

        String accessToken =
                jwtService.generateToken(user);

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        String redirect =
                redirectUri
                        + "?token="
                        + encode(accessToken)
                        + "&refreshToken="
                        + encode(refreshToken)
                        + "&id="
                        + encode(String.valueOf(user.getId()))
                        + "&name="
                        + encode(user.getName())
                        + "&email="
                        + encode(user.getEmail())
                        + "&role="
                        + encode(user.getRole());

        response.sendRedirect(redirect);
    }

    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}