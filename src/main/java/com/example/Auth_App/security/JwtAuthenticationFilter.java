 package com.example.Auth_App.security;

import com.example.Auth_App.helpers.UserHelpers;
import com.example.Auth_App.repositories.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        logger.info("Authorization header : {}", header);

        if(header != null && header.startsWith("Bearer ")){

            // token extract -> validate -> authentication create -> set inside security context

            String token = header.substring(7);


            try{
                // check access token
                if(!jwtService.isAccessToken(token)){
                    // message pass kar
                    filterChain.doFilter(request,response);
                    return;
                }

                Jws<Claims> parse = jwtService.parse(token);
                Claims payload = parse.getPayload();
                String userId =  payload.getSubject(); // as we stored userId in subject of jwt
                UUID userUuid = UserHelpers.parseUUID(userId);

                userRepository.findById(userUuid).ifPresent( user -> {



                    // user mil chuka hai, now we can fetch anything of user
                    if(user.isEnable()){
                        List<GrantedAuthority> authorities = user.getRoles() == null ? List.of() : user.getRoles()
                                .stream().map(role -> new SimpleGrantedAuthority(role.getName()))
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                authorities
                        );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // final line to set the authentication to security context
                        if(SecurityContextHolder.getContext().getAuthentication() == null){
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                });


            } catch (ExpiredJwtException e) {
                request.setAttribute("error", "token expired");
//                e.printStackTrace();
            } catch (Exception e){
                request.setAttribute("error", "token invalid");
//                e.printStackTrace();
            }

        }

        filterChain.doFilter(request,response); // it will forward the request
    }

    // this will disable jwt authenticate filter from login
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/api/v1/auth");
    }
}
