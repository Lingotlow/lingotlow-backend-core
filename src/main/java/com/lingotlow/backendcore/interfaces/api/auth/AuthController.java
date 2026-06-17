package com.lingotlow.backendcore.interfaces.api.auth;

import com.lingotlow.backendcore.infrastructure.security.JwtTokenProvider;
import com.lingotlow.backendcore.interfaces.dto.AuthRequest;
import com.lingotlow.backendcore.interfaces.dto.AuthResponse;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  @PostConstruct
  public void init() {
    log.info("✅✅✅ AuthController INITIALIZED at /auth ✅✅✅");
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    log.info("🔐 Login attempt for user: {}", request.getEmail());

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

      String token = tokenProvider.generateToken(authentication);
      log.info("✅ Login successful for user: {}", request.getEmail());
      return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    } catch (Exception e) {
      log.error("❌ Login failed for user: {}", request.getEmail(), e);
      throw e;
    }
  }
}
