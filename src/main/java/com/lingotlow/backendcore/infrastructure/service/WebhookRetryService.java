package com.lingotlow.backendcore.infrastructure.service;

import com.lingotlow.backendcore.domain.enums.EventStatus;
import com.lingotlow.backendcore.domain.event.model.Event;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryService {

    private static final String STREAM_RETRY = "stream:retry";
    private static final String STREAM_DLQ = "stream:dlq";
    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;

    @Transactional
    public void scheduleRetry(UUID eventId, int attemptNumber) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        event.setStatus(EventStatus.FAILED);
        event.setRetryCount(attemptNumber);
        event.setLastRetryAt(Instant.now());
        eventRepository.save(event);

        Map<String, Object> message = new HashMap<>();
        message.put("eventId", eventId.toString());
        message.put("attemptNumber", String.valueOf(attemptNumber));
        message.put("timestamp", Instant.now().toString());

        MapRecord<String, String, Object> record = StreamRecords.newRecord()
                .in(STREAM_RETRY)
                .ofMap(message)
                .withId(RecordId.autoGenerate());

        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.info("Event {} scheduled for retry (attempt {}) with id: {}", eventId, attemptNumber, recordId);
    }

    @Transactional
    public void moveToDlq(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        event.setStatus(EventStatus.DLQ);
        eventRepository.save(event);

        Map<String, Object> message = new HashMap<>();
        message.put("eventId", eventId.toString());
        message.put("documentId", event.getDocumentId());
        message.put("type", event.getType());
        message.put("failureReason", event.getFailureReason());
        message.put("timestamp", Instant.now().toString());

        MapRecord<String, String, Object> record = StreamRecords.newRecord()
                .in(STREAM_DLQ)
                .ofMap(message)
                .withId(RecordId.autoGenerate());

        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.warn("Event {} moved to DLQ with id: {}", eventId, recordId);
    }
}