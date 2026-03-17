package com.example.Auth_App.controllers;

import com.example.Auth_App.dtos.LoginRequest;
import com.example.Auth_App.dtos.RefreshTokenRequest;
import com.example.Auth_App.dtos.TokenResponse;
import com.example.Auth_App.dtos.UserDto;
import com.example.Auth_App.entities.RefreshToken;
import com.example.Auth_App.entities.User;
import com.example.Auth_App.repositories.RefreshTokenRepository;
import com.example.Auth_App.repositories.UserRepository;
import com.example.Auth_App.security.CookieService;
import com.example.Auth_App.security.JwtService;
import com.example.Auth_App.services.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;



    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        // authenticate
        Authentication authenticate = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if(!user.isEnable()){
            throw new DisabledException("User is disabled");
        }

        // refresh token
        String jti = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        // refresh token information saved in db
        refreshTokenRepository.save(refreshTokenOb);


        // GENERATE Access TOKEN
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        // use cookie service to attach refresh token in cookie
        cookieService.attachRefreshCookie(response, refreshToken, (int)jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response); // search for it

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);
    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try{
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        }catch (Exception e){
            throw new BadCredentialsException("Invalid username and password");
        }
    }

    // access and refresh token renew karne ke liye
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String refreshToken = readRefreshTokenRequest(body, request).orElseThrow(() -> new BadCredentialsException("Refresh token is missing"));

        if(!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti).orElseThrow(() -> new BadCredentialsException("Refresh Token not recognized"));
        if(storedRefreshToken.isRevoked()){
            throw new BadCredentialsException("Refresh Token expired or revoked");
        }
        if(storedRefreshToken.getExpiredAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Refresh Token expired");
        }

        if(!storedRefreshToken.getUser().getId().equals(userId)){
            throw new BadCredentialsException("Refresh Token does not belong to this User");
        }

        // refresh token ko rotate => remove stored RT and place new one
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();

        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        // save new token and generate new AT and RT
        refreshTokenRepository.save(newRefreshTokenOb);
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken1 = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());
        cookieService.attachRefreshCookie(response, newRefreshToken1, (int)jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);
        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken1, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class)));


    }

    // REVOKE after logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshTokenRequest(null, request).ifPresent(token -> {
            try{
                if(jwtService.isRefreshToken(token)){
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            } catch(JwtException ignored){

            }
        });

        // also clear cookies
        // Use CookieUtil (same behavior)
        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

//  this method will read refresh token from request header or body
    private Optional<String> readRefreshTokenRequest(RefreshTokenRequest body, HttpServletRequest request) {
            // prefer reading refresh token fromm cookie
        if(request.getCookies() != null){
            Optional<String> fromCookie =  Arrays.stream(request.getCookies()).filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    // only accept the cookies whose name matches with name of cookie in our cookieService we have
                    .map(Cookie::getValue) // only take the value of cookie
                    .filter(v -> !v.isBlank())
                    .findFirst();
            if(fromCookie.isPresent()){
                return fromCookie;
            }
        }
        // 2. body
        if(body != null && body.refreshToken() != null && !body.refreshToken().isBlank()){
            return Optional.of(body.refreshToken());
        }
        // optional not needed
        // 3. customer header can be done
        String refreshHeader = request.getHeader("X-Refresh_Token");
        if(refreshHeader!= null && !refreshHeader.isBlank()){
            return Optional.of(refreshHeader.trim());
        }

        // can also accept through Authorization = Bearer <token>
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {

            String candidate = authHeader.substring(7).trim();

            if (!candidate.isEmpty()) {

                try {
                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }
                } catch (Exception ignored) {
                }

            }
        }
        return Optional.empty();
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}
