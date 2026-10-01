package com.huyho.taskflow.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final RegistrationService registration;
    private final LoginService login;

    public AuthController(RegistrationService registration, LoginService login) {
        this.registration = registration;
        this.login = login;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a 15-minute bearer token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(login.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a User", description = "Password: at least 12 characters and at most 72 UTF-8 bytes. No login token is issued.")
    @ApiResponse(responseCode = "201", description = "User registered", useReturnTypeSchema = true)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registration.register(request));
    }
}
