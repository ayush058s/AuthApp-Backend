package com.example.Auth_App.security;

import com.example.Auth_App.entities.Role;
import com.example.Auth_App.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Getter
@Setter
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${spring.security.jwt.secret}") String secret,
            @Value("${spring.security.jwt.access-ttl-seconds}") long accessTtlSeconds
            ,@Value("${spring.security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds
            ,@Value("${spring.security.jwt.issuer}") String issuer) {

        if(secret == null || secret.length() < 64){
            throw new IllegalArgumentException("Invalid JWT Secret");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    // generate access token
    public String generateAccessToken(User user){
        Instant now = Instant.now();
        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ","access"
                ))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // generate refersh token
    public String generateRefreshToken(User user, String jti){
        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ", "refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // parse the token => When a client sends a token to your backend, the server must parse it to:
    //Verify the token is authentic.
    //Check it is not expired.
    //Extract user information (like userId or email).
    public Jws<Claims> parse(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    // check if token is access or refresh
    public boolean isAccessToken(String token){
        Claims c = parse(token).getPayload();
        return "access".equals(c.get("typ"));
    }

    public UUID getUserId(String token){
        Claims c = parse(token).getPayload();
        return UUID.fromString(c.getSubject());
    }

    // get Token id
    public String getJti(String token){
        return parse(token).getPayload().getId();
    }

    public List<String> getRoles(String token){
        Claims c = parse(token).getPayload();
        return (List<String>) c.get("roles");
//        return List.of(c.getSubject());
    }

    public String  getEmail(String token){
        Claims c = parse(token).getPayload();
        return (String) c.get("email");
    }
}
