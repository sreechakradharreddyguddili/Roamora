package com.weatherwise.auth;

import com.weatherwise.exception.DuplicateEmailException;
import com.weatherwise.security.JwtService;
import com.weatherwise.security.RefreshTokenService;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse register(
            RegisterRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new DuplicateEmailException(
                    "An account with this email already exists. Please sign in instead."
            );
        }

        User user = new User();

        user.setName(
                request.name().trim()
        );

        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setRole("USER");

        User savedUser =
                userRepository.save(user);

        String accessToken =
                jwtService.generateToken(
                        savedUser
                );

        String refreshToken =
                refreshTokenService.createRefreshToken(
                        savedUser
                );

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid email or password"
                                )
                        );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {

            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken =
                jwtService.generateToken(user);

        String refreshToken =
                refreshTokenService.createRefreshToken(
                        user
                );

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshToken
        );
    }

    public AuthResponse refresh(
            String refreshToken
    ) {

        RefreshToken storedToken =
                refreshTokenService.validate(
                        refreshToken
                );

        User user =
                storedToken.getUser();

        String newAccessToken =
                jwtService.generateToken(user);

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                newAccessToken,
                refreshToken
        );
    }

    public void logout(
            String refreshToken
    ) {

        refreshTokenService.revoke(
                refreshToken
        );
    }
}