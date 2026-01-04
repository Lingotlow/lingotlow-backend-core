package com.lingotlow.backendcore.domain.apikey.cryto;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyCryptoService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashApiKey(String apiKeyPlain) {
        return DigestUtils.md5DigestAsHex(apiKeyPlain.getBytes());
    }

    public boolean matches(String plain, String hash) {
        return hashApiKey(plain).equals(hash);
    }
}
