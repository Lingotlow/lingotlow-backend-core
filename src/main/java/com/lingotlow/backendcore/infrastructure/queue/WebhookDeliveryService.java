package com.lingotlow.backendcore.infrastructure.queue;

import com.lingotlow.backendcore.infrastructure.repository.entity.EndpointEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final RestTemplate restTemplate;

    public boolean deliverWebhook(EventEntity event, EndpointEntity endpoint) {
        try {
            log.debug("Delivering webhook: eventId={}, endpointId={}, url={}", 
                    event.getId(), endpoint.getId(), endpoint.getUrl());

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("User-Agent", "Lingotlow-Webhook/1.0");
            headers.add("X-Lingotlow-Event-Type", event.getType());
            headers.add("X-Lingotlow-Event-Id", event.getRequestId().toString());
            headers.add("X-Lingotlow-Tenant-Id", event.getTenantId().toString());
            headers.add("X-Lingotlow-Timestamp", String.valueOf(Instant.now().getEpochSecond()));

            // Add signature if secret key is configured
            if (endpoint.getSecretKey() != null && !endpoint.getSecretKey().isEmpty()) {
                String signature = generateSignature(event, endpoint.getSecretKey());
                headers.add("X-Lingotlow-Signature", "sha256=" + signature);
            }

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", event.getRequestId());
            payload.put("documentId", event.getDocumentId());
            payload.put("type", event.getType());
            payload.put("timestamp", event.getCreatedAt());
            payload.put("metadata", event.getMetadata());
            payload.put("headers", event.getHeaders());
            payload.put("payload", event.getPayload());
            payload.put("sourceIp", event.getSourceIp());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Make the HTTP request
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                log.info("Webhook delivered successfully: eventId={}, endpointId={}, status={}", 
                        event.getId(), endpoint.getId(), response.getStatusCode());
            } else {
                log.warn("Webhook delivery failed: eventId={}, endpointId={}, status={}, body={}", 
                        event.getId(), endpoint.getId(), response.getStatusCode(), response.getBody());
            }

            return success;

        } catch (Exception e) {
            log.error("Error delivering webhook: eventId={}, endpointId={}", 
                    event.getId(), endpoint.getId(), e);
            return false;
        }
    }

    private String generateSignature(EventEntity event, String secretKey) {
        try {
            String payload = String.format("%s.%s.%s.%s", 
                    event.getRequestId(),
                    event.getDocumentId(),
                    event.getType(),
                    event.getCreatedAt().toInstant().getEpochSecond());

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] signatureBytes = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating webhook signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
