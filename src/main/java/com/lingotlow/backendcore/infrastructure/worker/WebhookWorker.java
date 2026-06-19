package com.lingotlow.backendcore.infrastructure.worker;

import com.lingotlow.backendcore.domain.enums.EventStatus;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.service.DeliveryService;
import com.lingotlow.backendcore.infrastructure.service.WebhookRetryService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookWorker {

  private static final String STREAM_EVENTS = "stream:events";
  private static final String STREAM_RETRY = "stream:retry";
  private static final String STREAM_DLQ = "stream:dlq";
  private static final String CONSUMER_GROUP = "webhook-workers";
  private static final String CONSUMER_NAME = "worker-1";
  private final RedisTemplate<String, Object> redisTemplate;
  private final EventRepository eventRepository;
  private final DeliveryService deliveryService;
  private final WebhookRetryService retryService;

  @PostConstruct
  public void init() {
    try {
      createConsumerGroup(STREAM_EVENTS);
      createConsumerGroup(STREAM_RETRY);
    } catch (Exception e) {
      log.warn("Failed to create consumer groups: {}", e.getMessage());
    }
  }

  @Scheduled(fixedDelay = 1000)
  public void processEvents() {
    processStream(STREAM_EVENTS);
  }

  @Scheduled(fixedDelay = 5000)
  public void processRetries() {
    processStream(STREAM_RETRY);
  }

  @SuppressWarnings("unchecked")
  private void processStream(String streamKey) {
    try {
      Consumer consumer = Consumer.from(CONSUMER_GROUP, CONSUMER_NAME);
      StreamReadOptions options = StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1));

      List<MapRecord<String, Object, Object>> records =
          redisTemplate
              .opsForStream()
              .read(consumer, options, StreamOffset.create(streamKey, ReadOffset.from(">")));

      for (MapRecord<String, Object, Object> record : records) {
        try {
          Map<Object, Object> value = record.getValue();
          String eventIdStr = (String) value.get("eventId");

          if (eventIdStr == null) {
            log.warn("Message without eventId, acknowledging");
            redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
            continue;
          }

          UUID eventId = UUID.fromString(eventIdStr);
          log.info("Processing event: {}", eventId);

          var eventOpt = eventRepository.findById(eventId);
          if (eventOpt.isEmpty()) {
            log.warn("Event not found: {}, acknowledging", eventId);
            redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
            continue;
          }

          var event = eventOpt.get();
          if (event.getStatus() != EventStatus.RECEIVED
              && event.getStatus() != EventStatus.FAILED) {
            log.info("Event {} already processed, acknowledging", eventId);
            redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
            continue;
          }

          boolean delivered = deliveryService.deliverEvent(eventId);

          if (delivered) {
            log.info("Event {} delivered successfully", eventId);
            redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
          } else {
            var eventUpdated = eventRepository.findById(eventId).orElseThrow();
            int retryCount =
                eventUpdated.getRetryCount() != null ? eventUpdated.getRetryCount() : 0;
            int maxRetries = 3;

            if (retryCount < maxRetries) {
              log.info(
                  "Scheduling retry for event {}, attempt {}/{}",
                  eventId,
                  retryCount + 1,
                  maxRetries);
              retryService.scheduleRetry(eventId, retryCount + 1);
              redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
            } else {
              log.warn("Event {} failed after {} attempts, moving to DLQ", eventId, maxRetries);
              retryService.moveToDlq(eventId);
              redisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId());
            }
          }

        } catch (Exception e) {
          log.error("Error processing message: {}", e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      log.error("Error processing stream {}: {}", streamKey, e.getMessage());
    }
  }

  private void createConsumerGroup(String streamKey) {
    try {
      redisTemplate.opsForStream().createGroup(streamKey, CONSUMER_GROUP);
      log.info("Created consumer group: {} for stream: {}", CONSUMER_GROUP, streamKey);
    } catch (Exception e) {
      // Grupo já existe
    }
  }
}
