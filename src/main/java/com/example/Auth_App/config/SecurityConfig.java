package com.example.Auth_App.config;


import com.example.Auth_App.dtos.ApiError;
import com.example.Auth_App.security.JwtAuthenticationFilter;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.View;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // it helps to customize authentication and Which rooms require authorization
    //Which people are allowed inside

    //else Spring Security enables default security:
    // All endpoints require login
    //Basic authentication enabled
    //Default login page appears



    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private AuthenticationSuccessHandler successHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationSuccessHandler successHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.successHandler = successHandler;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, View error) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                        .cors(Customizer.withDefaults())
                // making session stateless
                        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(authorizeHttpRequests ->
                                authorizeHttpRequests
                                        .requestMatchers("/api/v1/auth/register").permitAll()
                                        .requestMatchers("/api/v1/auth/login").permitAll()
                                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                                        .requestMatchers("/api/v1/auth/logout").permitAll()
                                        .anyRequest().authenticated()
        )
                .oauth2Login(oauth2 ->
                        // create this function
                        oauth2.successHandler(successHandler) // if user login successfully with oauth
                                .failureHandler(null) // if login fails
                ).logout(AbstractHttpConfigurer::disable) // as this will be handled by jwt

                // this will run when any un authenticated person will try to access request
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, e) ->
                        // error message
                        {
//                            e.printStackTrace();
                            response.setStatus(401);
                            response.setContentType("application/json");
                            String message =  e.getMessage();
                            String errorr = (String) request.getAttribute("error");
                            if(errorr != null){
                                message = errorr;
                            }
//                            Map<String, String> errorMap = Map.of(
//                                    "message", message,
//                                    "statusCode", Integer.toString(401)
//                            );

                            var apiError = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized access", message, request.getRequestURI());
                            // converting message into json
                            var objectMapper = new ObjectMapper();
                            response.getWriter().write(objectMapper.writeValueAsString(apiError));

                        }
                        ))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//                .httpBasic(Customizer.withDefaults());
        // we will use this when we are using basic authentication not while jwt


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

//    @Bean
//    public UserDetailsService users() {
//        User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
//
//        UserDetails user1 =  userBuilder.username("ankit").password("ankit12345").roles("ADMIN").build();
//        UserDetails user2 =  userBuilder.username("ajay").password("ajay12345").roles("ADMIN").build();
//        UserDetails user3 =  userBuilder.username("pinku").password("pinku12345").roles("USER").build();
//
//        return new InMemoryUserDetailsManager(user1, user2, user3);
//    }
}
