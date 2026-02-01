# Event Implementation - Lingotlow Backend Core

## Overview

This implementation adds complete event ingestion functionality to the Lingotlow backend, following Domain-Driven Design (DDD) principles and the existing architectural patterns in the project.

## Implemented Structure

### 1. Infrastructure Layer

#### EventEntity
- **Location**: `src/main/java/com/lingotlow/backendcore/infrastructure/repository/entity/EventEntity.java`
- **Description**: JPA entity representing the `events` table in PostgreSQL
- **Features**:
  - Follows the specified schema exactly
  - Optimized indexes for performance
  - JSONB support for payload and headers
  - Proper data types (UUID, TIMESTAMPTZ, INET)

#### EventRepository
- **Location**: `src/main/java/com/lingotlow/backendcore/infrastructure/repository/EventRepository.java`
- **Description**: Spring Data JPA interface for database operations
- **Key Methods**:
  - `findByTenantIdAndDocumentId()` - For idempotency
  - `existsByTenantIdAndDocumentId()` - Quick verification
  - `findByRequestId()` - Search by request ID

### 2. Domain Layer

#### Domain Models
- **EventRequestDTO**: DTO for event data reception
- **EventResponseDTO**: DTO for client response
- **Validations**: Bean Validation annotations for required fields

#### EventServiceMapper
- **Location**: `src/main/java/com/lingotlow/backendcore/domain/event/mapper/EventServiceMapper.java`
- **Description**: MapStruct mapper for conversion between DTOs and entities
- **Features**:
  - JSON conversion for storage
  - Automatic requestId generation
  - Initial status definition (RECEIVED)

#### EventService
- **Location**: `src/main/java/com/lingotlow/backendcore/domain/event/EventService.java`
- **Description**: Domain service with business logic
- **Features**:
  - Tenant validation
  - DocumentId-based idempotency
  - Unique requestId generation (UUID v4)
  - Structured logging
  - Event auditing

### 3. Interface Layer

#### API Models
- **EventIngestRequest**: Model for HTTP requests
- **EventIngestResponse**: Model for HTTP responses

#### EventControllerMapper
- **Location**: `src/main/java/com/lingotlow/backendcore/interfaces/api/event/mapper/EventControllerMapper.java`
- **Description**: Mapper between API and domain models

#### EventController
- **Location**: `src/main/java/com/lingotlow/backendcore/interfaces/api/event/EventController.java`
- **Endpoint**: `POST /api/ingest/{tenantKey}`
- **Features**:
  - Input validation
  - Client IP extraction
  - Error handling
  - OpenAPI/Swagger documentation

### 4. Logging and Auditing

#### AuditLogger (Extended)
- **New Method**: `logEventCreated()`
- **Logged Data**: tenantKey, requestId, sourceIp, event details
- **Format**: Structured JSON for easy analysis

## Implemented Features

### ✅ Acceptance Criteria

1. **Table Created Correctly**
   - PostgreSQL schema as specified
   - Performance-optimized indexes
   - Proper data types

2. **Functional Ingestion Endpoint**
   - `POST /api/ingest/{tenantKey}`
   - Required field validation (documentId, type, timestamp)
   - Automatic requestId generation (UUID v4)
   - 200 response with requestId, status, and timestamp

3. **Error Handling**
   - 400: Data validation error
   - 404: Tenant not found
   - 409: Duplicate event (same documentId)
   - 500: Internal server errors

4. **Idempotency**
   - Based on documentId + tenantId
   - Prevents event duplication
   - Efficient verification before insertion

5. **Structured Logging**
   - Audit logs for each created event
   - Tenant, requestId, sourceIp information
   - JSON format for observability integration

## Implemented Tests

### 1. Unit Tests (EventServiceTest)
- ✅ Successful event creation
- ✅ Non-existent tenant handling
- ✅ Duplication prevention
- ✅ Creation without documentId
- ✅ Search by requestId
- ✅ Existence verification

### 2. Integration Tests (EventControllerIntegrationTest)
- ✅ Creation with valid data
- ✅ Required field validation
- ✅ Non-existent tenant handling
- ✅ Duplication prevention via API
- ✅ Creation with minimal fields
- ✅ Empty payload handling

## Usage Example

### Request
```bash
curl -X POST http://localhost:8080/api/ingest/my-tenant \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "invoice-12345",
    "type": "invoice.created",
    "timestamp": 1703123456789,
    "metadata": {
      "customer": "Acme Corp",
      "amount": 1500.00
    },
    "headers": {
      "X-Custom-Header": "value"
    },
    "payload": "Invoice data..."
  }'
```

### Success Response
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECEIVED",
  "timestamp": "2024-01-28T20:44:16.789Z",
  "message": "Event received successfully"
}
```

### Error Response (Duplication)
```json
{
  "message": "Event already exists with identifier: invoice-12345",
  "timestamp": "2024-01-28T20:44:16.789Z",
  "status": 409
}
```

## Table Schema

```sql
CREATE TABLE events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  request_id UUID NOT NULL UNIQUE,
  document_id TEXT,
  type TEXT,
  status TEXT NOT NULL, -- RECEIVED, ENQUEUED, PROCESSING, SENT, FAILED, DLQ
  created_at TIMESTAMPTZ DEFAULT now(),
  received_at TIMESTAMPTZ,
  s3_key TEXT,
  payload_json JSONB,
  headers JSONB,
  source_ip INET,
  attempts INT DEFAULT 0,
  last_attempt_at TIMESTAMPTZ,
  response_code INT,
  response_body TEXT
);

CREATE INDEX idx_events_tenant_status ON events (tenant_id, status);
CREATE INDEX idx_events_tenant_document ON events (tenant_id, document_id);
```

## Next Steps

1. **Asynchronous Processing**: Implement workers for event processing
2. **Automatic Retry**: Retry logic with exponential backoff
3. **S3 Integration**: Large payload storage
4. **Metrics**: Prometheus/Grafana integration
5. **Rate Limiting**: Per-tenant rate control
6. **Authentication**: API key validation

## Technologies Used

- **Java 21** with Spring Boot 3.1.4
- **PostgreSQL** with JSONB and UUID types
- **MapStruct** for object mapping
- **Bean Validation** for validation
- **Spring Data JPA** for persistence
- **TestContainers** for integration tests
- **RestAssured** for API tests
- **OpenAPI/Swagger** for documentation

## Standards Compliance

- ✅ **DDD**: Clear layer separation
- ✅ **Clean Architecture**: Dependencies point inward
- ✅ **SOLID**: Object-oriented design principles
- ✅ **Test-Driven**: Unit and integration test coverage
- ✅ **Documentation**: OpenAPI and documented code
