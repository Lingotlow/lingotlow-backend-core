# Message Queue Configuration (Upstash Redis Streams)

## Overview

This implementation configures the message queue using Upstash Redis Streams for asynchronous event processing, with robust error handling and idempotency guarantees.

## Configuration

### Environment Variables

- `QUEUE_URL`: Upstash Redis Streams connection URL (format: `redis://[password@]host:port/database`)
- `REDIS_HOST`: Redis host (alternative to QUEUE_URL)
- `REDIS_PORT`: Redis port (alternative to QUEUE_URL)
- `REDIS_PASSWORD`: Redis password (alternative to QUEUE_URL)
- `REDIS_DATABASE`: Redis database (default: 0)

### Configuration Example

```bash
# For Upstash
export QUEUE_URL="redis://password@your-upstash-host:port/database"

# For local Redis
export QUEUE_URL="redis://localhost:6379/0"
```

## Implemented Components

### 1. EventQueueProducer
- **Purpose**: Produces messages to the Redis Streams queue
- **Features**:
  - Retry with exponential backoff
  - Idempotency via Redis keys
  - Prometheus metrics (`ingest_enqueued_total`, `ingest_rejected_total`)
  - Queue health check
  - Detailed operation logging

### 2. QueueConfig
- **Purpose**: Configures Redis connection factory
- **Features**:
  - QUEUE_URL or individual configuration support
  - Configured connection pool
  - JSON serialization for objects

### 3. QueueFailureHandler
- **Purpose**: Failure handling and recovery
- **Features**:
  - Periodic health checks
  - Failed event reprocessing
  - Failure logging for monitoring

### 4. EventService Integration
- **Purpose**: Automatic queueing after persistence
- **Features**:
  - Non-blocking event creation even if queue fails
  - Queue status auditing
  - Complete payload with metadata

## Processing Flow

1. **Event Reception**: API receives event
2. **Validation**: Tenant and idempotency validated
3. **Persistence**: Event saved to database
4. **Queueing**: Event sent to queue (asynchronous)
5. **Processing**: Consumer processes event from queue

## Error Handling

### Queue Failures
- **Automatic retry**: 3 attempts with exponential backoff
- **Non-blocking**: Event still created even if queue fails
- **Metrics**: `ingest_rejected_total` incremented
- **Logging**: Detailed for troubleshooting

### Idempotency
- **Unique key**: `event:{requestId}` in Redis
- **TTL**: 24 hours to prevent accumulation
- **Verification**: Before queueing

## Prometheus Metrics

### Counters
- `ingest_enqueued_total`: Total events successfully enqueued
- `ingest_rejected_total`: Total events rejected

### Health Check
- Endpoint: `/actuator/health`
- Checks Redis Streams connectivity

## Tests

### Implemented Tests
1. **EventQueueProducerTest**: Unit tests with mocks
2. **EventServiceQueueIntegrationTest**: Integration with EventService
3. **QueueFailureSimulationTest**: Failure simulation
4. **QueueIntegrationTest**: Real Redis tests (Testcontainers)

### Running Tests
```bash
# Run all tests
mvn test

# Run only queue tests
mvn test -Dtest="*Queue*"
```

## Monitoring

### Important Logs
- `Event enqueued successfully`: Queueing success
- `Failed to enqueue event`: Queueing failure
- `Queue health check failed`: Connectivity issue
- `Event already processed (idempotent)`: Duplicate attempt

### Suggested Alerts
- High rate of `ingest_rejected_total`
- Queue health check failures
- Production error logs

## Performance

### Optimized Configurations
- **Pool size**: 10 maximum connections
- **Timeout**: 2 seconds for operations
- **Backoff**: Exponential (100ms, 200ms, 400ms)

### Considerations
- Large payloads may affect performance
- Monitor Redis memory usage
- Consider partitioning by tenant if high volume

## Security

### Recommendations
- Use TLS in production (Upstash already provides)
- Rotate passwords regularly
- Monitor suspicious access
- Limit payload size

## Deployment

### Production
1. Configure environment variables
2. Verify Upstash connectivity
3. Monitor initial metrics
4. Test failure scenarios

### Staging
- Use local Redis or separate instance
- Test with real data volume
- Validate retry and idempotency
