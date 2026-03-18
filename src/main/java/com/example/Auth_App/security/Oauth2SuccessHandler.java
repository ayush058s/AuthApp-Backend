package com.example.Auth_App.security;

import com.example.Auth_App.entities.Provider;
import com.example.Auth_App.entities.RefreshToken;
import com.example.Auth_App.entities.User;
import com.example.Auth_App.repositories.RefreshTokenRepository;
import com.example.Auth_App.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@AllArgsConstructor
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(Oauth2SuccessHandler.class);
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Successful authentication");
        logger.info(authentication.toString());

        OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();// converted the principal into OAuth user

        // identify user
        String registrationId = "unknown";
        if(authentication instanceof OAuth2AuthenticationToken token){
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("Registration Id: " + registrationId);
        logger.info("user : " + oAuth2User.getAttributes().toString());

        User user;
        switch (registrationId){
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();
                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();

                // build the user
                user = User.builder()
                        .email(email)
                        .name(name)
                        .enable(true)
                        .image(picture)
                        .provider(Provider.GOOGLE)
                        .build();
                // after this we will save user if not present
                userRepository.findByEmail(email).ifPresentOrElse(user1 -> {
                    logger.info("user is there in database");
                    logger.info(user1.toString());
                }, () -> {
                    // specify - default role
                    userRepository.save(user);
                });
            }
            default -> {
                throw new RuntimeException("Invalid registration id");
            }
        }



        // we are getting
        // username
        // user email

        // create new user
        // generate token -> and redirect to frontend

        // will provide refresh token instead of access token that will itself create new access token
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String accessToken =  jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        // NOW WE WILL ADD THIS REFRESH TOKEN TO COOKIE
        cookieService.attachRefreshCookie(response, refreshToken, (int)jwtService.getRefreshTtlSeconds());

        // then we will redirect to frontend

        response.getWriter().write("Login successful");
    }
}
