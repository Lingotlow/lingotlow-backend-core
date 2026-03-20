package com.lingotlow.backendcore.interfaces.api.auth;

import com.lingotlow.backendcore.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for user authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user with email and password and returns JWT token")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "Authentication successful",
                            content = @Content(schema = @Schema(implementation = JwtAuthResponse.class))),
                    @ApiResponse(
                            responseCode = "401", 
                            description = "Invalid credentials"),
                    @ApiResponse(
                            responseCode = "400", 
                            description = "Invalid request data")
            })
    public ResponseEntity<JwtAuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        
        log.info("User authenticated successfully: {}", loginRequest.getEmail());
        
        return ResponseEntity.ok(new JwtAuthResponse(jwt, tokenProvider.getExpirationInMs()));
    }

    @PostMapping("/validate")
    @Operation(
            summary = "Validate JWT token",
            description = "Validates if the provided JWT token is still valid")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "Token is valid"),
                    @ApiResponse(
                            responseCode = "401", 
                            description = "Token is invalid or expired")
            })
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        // Se chegou aqui, o token já foi validado pelo filtro JWT
        return ResponseEntity.ok().build();
    }

    // DTOs
    public static class LoginRequest {
        @Schema(description = "User email", example = "admin@lingotlow.com")
        private String email;
        
        @Schema(description = "User password", example = "admin123")
        private String password;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class JwtAuthResponse {
        @Schema(description = "JWT access token")
        private String accessToken;
        
        @Schema(description = "Token expiration time in milliseconds")
        private long expiresIn;

        public JwtAuthResponse(String accessToken, long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }

        // Getters
        public String getAccessToken() { return accessToken; }
        public long getExpiresIn() { return expiresIn; }
    }
}
