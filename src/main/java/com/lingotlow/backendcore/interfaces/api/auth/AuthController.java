package com.lingotlow.backendcore.interfaces.api.auth;

import com.lingotlow.backendcore.domain.enums.UserRole;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.domain.user.model.AppUser;
import com.lingotlow.backendcore.infrastructure.repository.AppUserRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.security.JwtTokenProvider;
import com.lingotlow.backendcore.interfaces.dto.AuthRequest;
import com.lingotlow.backendcore.interfaces.dto.AuthResponse;
import com.lingotlow.backendcore.interfaces.dto.SignUpRequest;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ AuthController INITIALIZED at /auth ✅✅✅");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("🔐 Login attempt for user: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);
            log.info("✅ Login successful for user: {}", request.getEmail());
            return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
        } catch (Exception e) {
            log.error("❌ Login failed for user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignUpRequest request) {
        log.info("📝 Signup attempt for user: {}", request.getEmail());

        // Verificar se o usuário já existe
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("❌ User already exists: {}", request.getEmail());
            Map<String, String> error = new HashMap<>();
            error.put("error", "User already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Validar senha
        if (request.getPassword().length() < 8) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Password must be at least 8 characters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Passwords do not match");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Criar tenant para o usuário
        String emailPrefix = request.getEmail().split("@")[0];
        String tenantKey = "tenant-" + emailPrefix + "-" + UUID.randomUUID().toString().substring(0, 4);
        
        // Garantir que o tenantKey seja válido (sem caracteres especiais)
        tenantKey = tenantKey.replace(".", "-").replace("_", "-").toLowerCase();
        
        String tenantName = request.getName() != null && !request.getName().isEmpty() 
                ? request.getName() + "'s Workspace" 
                : emailPrefix + "'s Workspace";

        Tenant tenant = Tenant.builder()
                .tenantKey(tenantKey)
                .name(tenantName)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("✅ Tenant created: {} for user: {}", tenantKey, request.getEmail());

        // Criar o usuário com o tenant associado
        AppUser user = AppUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .tenant(savedTenant)
                .build();

        userRepository.save(user);
        log.info("✅ User created successfully: {} with tenant: {}", request.getEmail(), tenantKey);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("email", request.getEmail());
        response.put("tenantKey", tenantKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
