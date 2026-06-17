package com.lingotlow.backendcore.domain.user;

import com.lingotlow.backendcore.domain.enums.UserRole;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.domain.user.model.AppUser;
import com.lingotlow.backendcore.infrastructure.repository.AppUserRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .roles(appUser.getRole().name())
                .build();
    }

    @Transactional
    public AppUser createUser(String email, String password, UserRole role, String tenantKey) {
        Tenant tenant = null;
        if (tenantKey != null && !tenantKey.isEmpty()) {
            tenant = tenantRepository.findByTenantKey(tenantKey)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));
        }

        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .tenant(tenant)
                .build();

        AppUser savedUser = userRepository.save(user);
        log.info("User created: {}", email);
        return savedUser;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}