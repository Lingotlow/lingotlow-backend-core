# Tenant Management Guide - Lingotlow Backend

## Overview

This guide covers the complete tenant management system in Lingotlow Backend, including API endpoints, data models, and implementation details following Domain-Driven Design (DDD) principles.

## Architecture

### Domain Layer
- **TenantService**: Core business logic for tenant operations
- **TenantRequestDTO**: Input model for tenant creation
- **TenantUpdateDTO**: Input model for tenant updates
- **TenantServiceMapper**: MapStruct mapper for domain conversions

### Infrastructure Layer
- **TenantEntity**: JPA entity representing tenants table
- **TenantRepository**: Spring Data JPA repository
- **TenantMetrics**: Metrics collection for tenant operations
- **AuditLogger**: Audit logging for tenant activities

### Interface Layer
- **TenantController**: REST API endpoints
- **TenantCreateRequest**: API input model for creation
- **TenantResponse**: API output model
- **TenantSummary**: Lightweight tenant representation

## API Endpoints

### 1. Create Tenant
```http
POST /api/tenants
Content-Type: application/json

{
  "tenantKey": "acme-corp",
  "name": "Acme Corporation",
  "config": "{\"webhookUrl\": \"https://acme.com/webhook\"}"
}
```

**Response:**
```http
201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantKey": "acme-corp",
  "name": "Acme Corporation",
  "config": "{\"webhookUrl\": \"https://acme.com/webhook\"}",
  "createdAt": "2024-01-28T20:44:16.789Z",
  "updatedAt": "2024-01-28T20:44:16.789Z"
}
```

### 2. List Tenants
```http
GET /api/tenants?page=0&size=20
```

**Response:**
```http
200 OK
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "tenantKey": "acme-corp",
      "name": "Acme Corporation",
      "createdAt": "2024-01-28T20:44:16.789Z",
      "updatedAt": "2024-01-28T20:44:16.789Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 3. Get Tenant Details
```http
GET /api/tenants/{tenantKey}
```

**Response:**
```http
200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantKey": "acme-corp",
  "name": "Acme Corporation",
  "config": "{\"webhookUrl\": \"https://acme.com/webhook\"}",
  "createdAt": "2024-01-28T20:44:16.789Z",
  "updatedAt": "2024-01-28T20:44:16.789Z"
}
```

### 4. Update Tenant
```http
PUT /api/tenants/{tenantKey}
Content-Type: application/json

{
  "name": "Acme Corporation Updated",
  "config": "{\"webhookUrl\": \"https://acme.com/webhook-updated\"}"
}
```

**Response:**
```http
200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantKey": "acme-corp",
  "name": "Acme Corporation Updated",
  "config": "{\"webhookUrl\": \"https://acme.com/webhook-updated\"}",
  "createdAt": "2024-01-28T20:44:16.789Z",
  "updatedAt": "2024-01-28T21:00:00.000Z"
}
```

### 5. Delete Tenant
```http
DELETE /api/tenants/{tenantKey}
```

**Response:**
```http
204 No Content
```

## Data Models

### TenantEntity
```java
@Entity
@Table(name = "tenants", indexes = @Index(name = "idx_tenants_key", columnList = "tenantKey"))
public class TenantEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_key", unique = true, nullable = false) private String tenantKey;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "jsonb") private String config;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
}
```

### TenantRequestDTO
```java
@Data
public class TenantRequestDTO {
    private String tenantKey;
    private String name;
    private String config;
}
```

### TenantResponse
```java
@Data
public class TenantResponse {
    private UUID id;
    private String tenantKey;
    private String name;
    private String config;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

## Database Schema

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenants_key ON tenants(tenant_key);

-- Trigger for automatic updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at 
    BEFORE UPDATE ON tenants 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
```

## Business Logic

### Tenant Creation
1. **Validation**: Check if tenantKey already exists
2. **Entity Creation**: Map DTO to entity
3. **Timestamps**: Set createdAt and updatedAt
4. **Persistence**: Save to database
5. **Auditing**: Log tenant creation
6. **Metrics**: Increment tenant creation counter

### Tenant Update
1. **Validation**: Check if tenant exists
2. **Entity Mapping**: Map update DTO to existing entity
3. **Timestamp Update**: Update updatedAt field
4. **Persistence**: Save changes
5. **Auditing**: Log tenant update
6. **Metrics**: Increment tenant update counter

### Tenant Deletion
1. **Validation**: Check if tenant exists
2. **Deletion**: Remove from database
3. **Auditing**: Log tenant deletion
4. **Metrics**: Increment tenant deletion counter

## Error Handling

### Validation Errors (400)
```json
{
  "message": "Validation failed",
  "errors": [
    {
      "field": "tenantKey",
      "message": "Tenant key is required"
    },
    {
      "field": "name",
      "message": "Name is required"
    }
  ],
  "timestamp": "2024-01-28T20:44:16.789Z",
  "status": 400
}
```

### Conflict Errors (409)
```json
{
  "message": "Tenant already exists with key: acme-corp",
  "timestamp": "2024-01-28T20:44:16.789Z",
  "status": 409
}
```

### Not Found Errors (404)
```json
{
  "message": "Tenant not found with key: non-existent",
  "timestamp": "2024-01-28T20:44:16.789Z",
  "status": 404
}
```

## Security

### Authentication
- All endpoints require `ADMIN` role
- JWT token validation
- API key authentication for external access

### Authorization
- Only admin users can manage tenants
- Tenant isolation based on tenantKey
- Row-level security for data access

### Data Protection
- Sensitive data encrypted in database
- Audit logging for all operations
- Input validation and sanitization

## Testing

### Unit Tests
```bash
# Run tenant service tests
./mvnw test -Dtest=TenantServiceTest

# Test specific methods
./mvnw test -Dtest=TenantServiceTest#createTenant_Success_WhenTenantKeyDoesNotExist
```

### Integration Tests
```bash
# Run tenant controller tests
./mvnw test -Dtest=TenantControllerIntegrationTest

# Test with TestContainers
./mvnw test -Dtest=TenantControllerIntegrationTest#testCreateTenant_Success
```

### Test Coverage
- ✅ Tenant creation with valid data
- ✅ Tenant creation with duplicate key
- ✅ Tenant update scenarios
- ✅ Tenant deletion
- ✅ Tenant listing with pagination
- ✅ Error handling scenarios
- ✅ Security and authorization

## Monitoring

### Metrics
- `tenant.create.total`: Total tenants created
- `tenant.update.total`: Total tenants updated
- `tenant.delete.total`: Total tenants deleted
- `tenant.list.total`: Total tenant list requests
- `tenant.read.total`: Total tenant read requests

### Audit Logs
```json
{
  "timestamp": "2024-01-28T20:44:16.789Z",
  "eventType": "TENANT_OPERATION",
  "operation": "CREATE",
  "tenantKey": "acme-corp",
  "userId": "admin",
  "details": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Acme Corporation",
    "createdAt": "2024-01-28T20:44:16.789Z"
  },
  "service": "tenant-service"
}
```

### Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check tenant-specific metrics
curl http://localhost:8080/actuator/metrics/tenant.create.total
```

## Configuration

### Application Properties
```yaml
# Tenant configuration
tenant:
  default-config: |
    {
      "webhookUrl": "",
      "retryPolicy": {
        "maxAttempts": 3,
        "backoffMs": 1000
      },
      "rateLimit": {
        "requestsPerMinute": 1000
      }
    }
  
  validation:
    tenant-key:
      pattern: "^[a-z0-9-]{3,50}$"
      message: "Tenant key must be 3-50 characters, lowercase, numbers and hyphens only"
    name:
      min-length: 3
      max-length: 100
      message: "Name must be between 3 and 100 characters"
```

## Best Practices

### Tenant Key Generation
- Use lowercase, numbers, and hyphens only
- Make it meaningful and readable
- Avoid special characters and spaces
- Keep it between 3-50 characters

### Configuration Management
- Store configuration as JSONB for flexibility
- Validate configuration schema
- Provide default configuration templates
- Support configuration versioning

### Performance Optimization
- Use database indexes on tenantKey
- Implement caching for frequently accessed tenants
- Use pagination for tenant listing
- Optimize JSONB queries with GIN indexes

## Troubleshooting

### Common Issues

#### Tenant Key Already Exists
```bash
# Check if tenant exists
curl http://localhost:8080/api/tenants/acme-corp

# Generate unique key
uuidgen | head -c 8 | tr '[:upper:]' '[:lower:]'
```

#### Database Connection Issues
```bash
# Check database connection
docker exec -it postgres-lingotlow psql -U lingotlow -d lingotlow_dev

# Check tenants table
SELECT * FROM tenants WHERE tenant_key = 'acme-corp';
```

#### Performance Issues
```bash
# Check slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
WHERE query LIKE '%tenants%' 
ORDER BY mean_time DESC;

# Check index usage
SELECT schemaname,tablename,attname,n_distinct,correlation 
FROM pg_stats 
WHERE tablename = 'tenants';
```

## Migration Guide

### Adding New Tenant Fields
```sql
-- V4__Add_tenant_fields.sql
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Add new index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tenants_status ON tenants(status);
```

### Updating Tenant Configuration Schema
```java
// Add validation for new configuration fields
@Schema(description = "Tenant configuration")
public class TenantConfig {
    @NotBlank private String webhookUrl;
    @Valid private RetryPolicy retryPolicy;
    @Valid private RateLimit rateLimit;
}
```

## API Documentation

### Swagger UI
Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification
Complete API specification available at:
```
http://localhost:8080/v3/api-docs
```

## Integration Examples

### JavaScript/Node.js
```javascript
const createTenant = async (tenantData) => {
  const response = await fetch('/api/tenants', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(tenantData)
  });
  
  if (!response.ok) {
    throw new Error(`Failed to create tenant: ${response.statusText}`);
  }
  
  return await response.json();
};

// Usage
const tenant = await createTenant({
  tenantKey: 'my-company',
  name: 'My Company Ltd',
  config: {
    webhookUrl: 'https://mycompany.com/webhook'
  }
});
```

### Python
```python
import requests
import json

def create_tenant(tenant_data, token):
    url = "http://localhost:8080/api/tenants"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }
    
    response = requests.post(url, json=tenant_data, headers=headers)
    response.raise_for_status()
    
    return response.json()

# Usage
tenant = create_tenant({
    "tenantKey": "my-company",
    "name": "My Company Ltd",
    "config": {
        "webhookUrl": "https://mycompany.com/webhook"
    }
}, token)
```

---

**Happy Tenant Management! 🚀**
