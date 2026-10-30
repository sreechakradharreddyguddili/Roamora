package com.weatherwise.oauth2;

import com.weatherwise.auth.User;
import com.weatherwise.auth.UserRepository;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private final DefaultOAuth2UserService delegate =
            new DefaultOAuth2UserService();

    public CustomOAuth2UserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {

        OAuth2User oauthUser =
                delegate.loadUser(userRequest);

        String emailAttribute =
                oauthUser.getAttribute("email");

        String name =
                oauthUser.getAttribute("name");

        if (emailAttribute == null || emailAttribute.isBlank()) {

            OAuth2Error error =
                    new OAuth2Error(
                            "invalid_user_info",
                            "Google account did not provide an email address",
                            null
                    );

            throw new OAuth2AuthenticationException(error);
        }

        final String email =
                emailAttribute.trim().toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseGet(() -> {

                            User newUser = new User();

                            newUser.setName(
                                    name != null && !name.isBlank()
                                            ? name
                                            : email.split("@")[0]
                            );

                            newUser.setEmail(email);

                            /*
                             * OAuth2 users do not authenticate
                             * using the local password.
                             */
                            newUser.setPassword("");

                            newUser.setRole("USER");

                            return userRepository.save(newUser);
                        });

        return oauthUser;
    }
}