# API Keys Management Implementation

## Overview

This implementation provides a complete REST API for managing API Keys associated with tenants, following security best practices and the requirements specified.

## Features Implemented

### ✅ Core Functionality
- **Create API Key**: `POST /api/tenants/{tenantKey}/api-keys`
- **List API Keys**: `GET /api/tenants/{tenantKey}/api-keys` (without exposing secrets)
- **Revoke API Key**: `DELETE /api/tenants/{tenantKey}/api-keys/{keyId}`
- **Validate API Key**: Internal method for authentication

### ✅ Security Features
- **Dual Authentication**: JWT for admin operations + API Key for service access
- **API Key Filter**: `ApiKeyAuthenticationFilter` validates keys on every request
- **Secure Key Storage**: API keys are hashed using BCrypt (never stored in plain text)
- **Key Generation**: Cryptographically secure random keys with `lt_` prefix
- **Tenant Isolation**: Keys are properly scoped to their respective tenants
- **Role-Based Access**: ADMIN role for key management, API_USER role for service access

### ✅ Error Handling
- **201 Created**: Successful API key creation
- **200 OK**: Successful listing
- **204 No Content**: Successful revocation
- **401 Unauthorized**: Authentication failures
- **403 Forbidden**: Authorization failures  
- **404 Not Found**: Tenant or API key not found
- **400 Bad Request**: Invalid input data

### ✅ Database Schema
- PostgreSQL table with proper indexes
- Foreign key constraints with cascade delete
- UUID primary keys for security
- Proper column types and constraints

## Architecture Components

### 🔐 ApiKeyCryptoService (Security Core)
Essential service for API key security operations:

**Functions:**
- **`generateApiKey()`** - Generates secure keys with `lt_` prefix
  - Uses `SecureRandom` for 32 random bytes
  - Base64 URL-safe encoding
  - Example: `lt_abc123def456...`

- **`hashApiKey()`** - Encrypts keys before database storage
  - Uses BCrypt (same algorithm as passwords)
  - Automatic salt generation
  - **Never stores plain text keys**

- **`matches()`** - Validates provided key against stored hash
  - Used by `validateApiKey()` method
  - Secure BCrypt comparison

### 🛡️ Security Classes

#### ApiKeyAuthenticationFilter
- **Purpose**: Intercepts all requests to validate API keys
- **Flow**:
  1. Extracts key from `X-API-Key` header or `api_key` parameter
  2. Identifies tenant from URL path (`/api/tenants/{tenantKey}/...`)
  3. Calls `validateApiKey()` for verification
  4. Sets authentication if valid

#### ApiKeyAuthenticationToken
- **Purpose**: Represents API key authentication in Spring Security
- **Contains**: tenantId, apiKey (credentials), `API_USER` role
- **Used by filter** to establish security context

#### SecurityConfig
- **Purpose**: Configures Spring Security with dual authentication
- **Rules**:
  - JWT + `ADMIN` role for API key management
  - API key + `API_USER` role for endpoint access
  - Public endpoints (auth, actuator, swagger)

### 🔄 Complete Authentication Flow

```
Request with API Key
       ↓
ApiKeyAuthenticationFilter
       ↓
Extract key + tenant from URL
       ↓
Call validateApiKey(tenantId, key)
       ↓
validateApiKey() uses cryptoService.matches()
       ↓
If valid → ApiKeyAuthenticationToken
       ↓
SecurityContext configured
       ↓
Request authorized
```

**ApiKeyCryptoService is the security heart** - without it there would be no secure key generation or validation!

## Architecture

### Domain Layer
```
domain/apikey/
├── ApiKeyService.java          # Core business logic
├── ApiKeyCryptoService.java    # Security operations
├── mapper/
│   └── ApiKeyServiceMapper.java # Domain mappings
└── model/
    └── ApiKeyResponseDTO.java  # Transfer objects
```

### Infrastructure Layer
```
infrastructure/
├── repository/
│   ├── ApiKeyRepository.java   # JPA repository
│   └── entity/
│       └── ApiKeyEntity.java   # Database entity
├── repository/impl/custom/
│   ├── ApiKeyRepositoryCustom.java    # Custom queries
│   └── ApiKeyRepositoryCustomImpl.java # Implementation
└── security/
    ├── ApiKeyAuthenticationFilter.java # API key validation filter
    ├── ApiKeyAuthenticationToken.java  # Custom authentication token
    └── SecurityConfig.java             # Spring Security configuration
```

### Interface Layer
```
interfaces/api/apikey/
├── ApiKeyController.java      # REST endpoints
├── mapper/
│   └── ApiKeyControllerMapper.java # Response mappings
└── model/
    ├── ApiKeyResponse.java    # List response model
    └── CreateApiKeyResponse.java # Creation response model
```

## Security Implementation

### Key Generation & Storage
- **Generation**: `SecureRandom` with Base64 URL-safe encoding
- **Prefix**: All keys start with `lt_` for easy identification
- **Hashing**: BCrypt with automatic salt generation
- **Storage**: Only hashed values stored in database

### Authentication & Authorization
- **Dual Authentication System**:
  - **JWT Tokens**: For administrative operations (key management)
  - **API Keys**: For service-to-service authentication
- **API Key Validation**: Automatic validation via `ApiKeyAuthenticationFilter`
- **Role-Based Access Control**:
  - `ADMIN`: Can create, list, and revoke API keys
  - `API_USER`: Can access protected endpoints with valid API key
- **Tenant Isolation**: Proper tenant resolution and validation
- **Security Headers**: API keys can be passed via `X-API-Key` header or `api_key` parameter

## API Documentation

### Create API Key
```http
POST /api/tenants/{tenantKey}/api-keys
Authorization: Bearer <jwt_token>
Content-Type: application/json

Response:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "apiKey": "lt_abc123def456...",
  "createdAt": "2026-01-28T12:30:00Z"
}
```

### List API Keys
```http
GET /api/tenants/{tenantKey}/api-keys
Authorization: Bearer <jwt_token>

Response:
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-01-28T12:30:00Z",
    "revoked": false
  }
]
```

### Revoke API Key
```http
DELETE /api/tenants/{tenantKey}/api-keys/{keyId}
Authorization: Bearer <jwt_token>

Response: 204 No Content
```

## Testing

### Unit Tests
- **ApiKeyServiceTest**: Complete unit test coverage for business logic
- Tests for creation, listing, revocation, and validation
- Edge cases and error conditions covered

### Integration Tests  
- **ApiKeyControllerTest**: Full endpoint testing
- Authentication and authorization testing
- Error response validation

## Logging & Monitoring

### Structured Logging
- **INFO**: Key operations (create, list, revoke)
- **WARN**: Validation failures and security issues
- **DEBUG**: Detailed validation information
- **ERROR**: Unexpected errors with stack traces

### Audit Trail
- All key operations are logged with tenant context
- Security events are properly tracked
- Performance metrics integration ready

## Database Migration

The implementation includes Flyway migration `V2__Create_api_keys_table.sql` with:
- Proper table creation with constraints
- Performance indexes
- Documentation comments
- Foreign key relationships

## Configuration Requirements

### Dependencies
- Spring Security (for JWT and BCrypt)
- Spring Data JPA (for repository operations)
- MapStruct (for object mapping)
- Validation (for input validation)

### Security Configuration
Ensure your Spring Security configuration includes:
- JWT authentication filter
- Role-based authorization
- CSRF protection for state-changing operations

## Usage Examples

### Using API Keys for Authentication
```bash
# Using API Key in header
curl -X GET http://localhost:8080/api/tenants/my-tenant/some-protected-endpoint \
  -H "X-API-Key: lt_abc123def456..."

# Using API Key as query parameter
curl -X GET "http://localhost:8080/api/tenants/my-tenant/some-protected-endpoint?api_key=lt_abc123def456..."
```

### Creating an API Key
```bash
curl -X POST http://localhost:8080/api/tenants/my-tenant/api-keys \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

### Listing API Keys
```bash
curl -X GET http://localhost:8080/api/tenants/my-tenant/api-keys \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Revoking an API Key
```bash
curl -X DELETE http://localhost:8080/api/tenants/my-tenant/api-keys/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Future Enhancements

### Potential Improvements
- **Key Expiration**: Add TTL support for API keys
- **Key Rotation**: Automatic rotation policies
- **Rate Limiting**: Per-key rate limiting
- **Usage Analytics**: Track API key usage patterns
- **Key Scopes**: Different permission levels per key
- **Webhook Support**: Notifications for key events

### Performance Considerations
- **Caching**: Cache tenant lookups for better performance
- **Batch Operations**: Support for bulk operations
- **Pagination**: For large numbers of API keys

## Compliance & Standards

This implementation follows:
- **OWASP Security Guidelines**: Proper key management practices
- **REST Best Practices**: Proper HTTP methods and status codes
- **Spring Boot Conventions**: Standard patterns and configurations
- **Database Design Principles**: Proper normalization and indexing

---

**Note**: This implementation assumes you have proper JWT authentication and tenant management already set up in your application.
