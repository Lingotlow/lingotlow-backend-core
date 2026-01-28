package com.lingotlow.backendcore.domain.apikey.cryto;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyCryptoService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "lt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashApiKey(String apiKeyPlain) {
        return passwordEncoder.encode(apiKeyPlain);
    }

    public boolean matches(String plain, String hash) {
        return passwordEncoder.matches(plain, hash);
    }
}
