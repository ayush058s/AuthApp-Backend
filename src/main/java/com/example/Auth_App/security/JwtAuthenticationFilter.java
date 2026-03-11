package com.example.Auth_App.security;

import com.example.Auth_App.entities.User;
import com.example.Auth_App.helpers.UserHelpers;
import com.example.Auth_App.repositories.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.awt.font.GraphicAttribute;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")){

            // token extract -> validate -> authentication create -> set inside security context

            String token = header.substring(7);

            // check access token
            if(!jwtService.isAccessToken(token)){
                // message pass kar
                filterChain.doFilter(request,response);
                return;
            }
            try{

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
                e.printStackTrace();
            } catch (MalformedJwtException e){
                e.printStackTrace();
            } catch (JwtException e){
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }

        }

        filterChain.doFilter(request,response); // it will forward the request
    }
}
