package com.huyho.taskflow.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        // Bearer tokens arrive in Authorization headers, not automatically sent cookies.
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health", "/actuator/health/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated());
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, exception) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType("application/problem+json");
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                    "A valid bearer token is required");
            problem.setInstance(URI.create(request.getRequestURI()));
            mapper.writeValue(response.getOutputStream(), problem);
        };
        http.exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/problem+json");
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
                    problem.setInstance(URI.create(request.getRequestURI()));
                    mapper.writeValue(response.getOutputStream(), problem);
                }))
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> {}).authenticationEntryPoint(entryPoint));
        return http.build();
    }
}
