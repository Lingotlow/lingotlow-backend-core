# Complete Security Filters Guide - API Keys

## 🎯 Overview

This document provides a detailed explanation of how security filters work in the project, including API key application and the Spring Security role-based system.

## 🔄 How Filters Work in Spring Boot

### Automatic Functionality

Spring Boot works by "convention over configuration":

1. **`@Component`** = Spring automatically registers the bean
2. **`@Configuration`** = Spring loads it on startup
3. **`@EnableWebSecurity`** = Enables web security
4. **Dependency Injection** = Spring connects everything automatically

### Real Flow When Application Starts

```
1. Spring scans @Component classes
   ↓
2. Finds ApiKeyAuthenticationFilter (@Component)
   ↓
3. Finds SecurityConfig (@Configuration)
   ↓
4. Injects ApiKeyAuthenticationFilter into SecurityConfig
   ↓
5. Executes .addFilterBefore() → Registers filter
   ↓
6. Filter starts intercepting ALL requests
```

## 📋 Exact Application Rules

### 🔓 PUBLIC Endpoints (don't go through filter)

```java
.requestMatchers("/api/auth/**").permitAll()        // Login, registration
.requestMatchers("/actuator/**").permitAll()        // Health checks
.requestMatchers("/swagger-ui/**").permitAll()      // Documentation
.requestMatchers("/v3/api-docs/**").permitAll()      // OpenAPI
```

**shouldNotFilter() - Completely skips:**
```java
return path.startsWith("/api/auth/") || 
       path.startsWith("/actuator/") ||
       path.startsWith("/swagger-ui/") ||
       path.startsWith("/v3/api-docs/");
```

### 🔐 Endpoints that REQUIRE JWT + ADMIN (API Key Management)

```java
.requestMatchers("/api/tenants/*/api-keys/**").hasRole("ADMIN")
```

**Specific endpoints:**
- `POST /api/tenants/{tenant}/api-keys` - Create API key
- `GET /api/tenants/{tenant}/api-keys` - List API keys  
- `DELETE /api/tenants/{tenant}/api-keys/{id}` - Revoke API key

### 🔑 Endpoints that ACCEPT JWT OR API KEY

```java
.requestMatchers("/api/**").hasAnyRole("ADMIN", "API_USER")
```

**ALL other `/api/**` endpoints can be accessed in two ways:**
- With JWT (any role)
- With API key (API_USER role)

## 🎭 Role System (HasRole vs HasAnyRole)

### 📖 Detailed Role Explanation

#### `hasRole("ROLE_NAME")`
- **Requires exactly that role**
- **Spring automatically adds "ROLE_" prefix**
- Example: `hasRole("ADMIN")` = looks for `ROLE_ADMIN`

#### `hasAnyRole("ROLE1", "ROLE2", ...)`
- **Accepts any of the listed roles**
- **Spring automatically adds "ROLE_" prefix**
- Example: `hasAnyRole("ADMIN", "API_USER")` = accepts `ROLE_ADMIN` OR `ROLE_API_USER`

#### `permitAll()`
- **Allows access without authentication**
- **Doesn't check any role**

#### `authenticated()`
- **Requires user to be authenticated**
- **Doesn't check specific role**

### 🏷️ Roles Implemented in the Project

#### `ROLE_ADMIN`
- **Purpose:** Administrative management
- **Can:** Create, list, revoke API keys
- **Authentication:** JWT token required
- **Endpoints:** `/api/tenants/*/api-keys/**`

#### `ROLE_API_USER`
- **Purpose:** Access via API key
- **Can:** Access protected endpoints with API key
- **Authentication:** Valid API key
- **Endpoints:** `/api/**` (except key management)

### 🔄 How Filter Defines Roles

#### API Key Authentication
```java
// In ApiKeyAuthenticationFilter
ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(tenantId, apiKey);
// → Automatically receives ROLE_API_USER
```

#### JWT Authentication
```java
// JWT already comes with user roles
// → Can be ROLE_ADMIN or others
```

## 📊 Access Summary Table

| Endpoint | Skips Filter? | Required Authentication | Required Role | How to Access |
|----------|--------------|------------------------|---------------|--------------|
| `/api/auth/**` | ✅ Yes | None | None | Public |
| `/actuator/**` | ✅ Yes | None | None | Public |
| `/swagger-ui/**` | ✅ Yes | None | None | Public |
| `/api/tenants/*/api-keys/**` | ❌ No | JWT | ADMIN | Header: `Authorization: Bearer <jwt>` |
| `/api/anything-else` | ❌ No | JWT OR API KEY | ADMIN or API_USER | Header: `Authorization: Bearer` **OR** `X-API-Key` |

## 🧪 Practical Usage Examples

### 1. Public Endpoints
```bash
# No authentication needed
curl -X POST http://localhost:8080/api/auth/login
curl -X GET http://localhost:8080/actuator/health
```

### 2. API Key Management (requires JWT ADMIN)
```bash
# Create API key
curl -X POST http://localhost:8080/api/tenants/tenant1/api-keys \
  -H "Authorization: Bearer <jwt_admin_token>"

# List API keys
curl -X GET http://localhost:8080/api/tenants/tenant1/api-keys \
  -H "Authorization: Bearer <jwt_admin_token>"

# Revoke API key
curl -X DELETE http://localhost:8080/api/tenants/tenant1/api-keys/key-id \
  -H "Authorization: Bearer <jwt_admin_token>"
```

### 3. Normal Endpoints (accepts API key or JWT)
```bash
# With API key
curl -X GET http://localhost:8080/api/tenants/tenant1/data \
  -H "X-API-Key: lt_abc123def456..."

# With JWT (any role)
curl -X GET http://localhost:8080/api/tenants/tenant1/data \
  -H "Authorization: Bearer <jwt_token>"
```

### 4. API Key as Parameter
```bash
# Also works as query parameter
curl -X GET "http://localhost:8080/api/tenants/tenant1/data?api_key=lt_abc123def456..."
```

## 🔄 Complete Authentication Flow

### With API Key
```
Request with X-API-Key
       ↓
ApiKeyAuthenticationFilter intercepts
       ↓
shouldNotFilter()? → No (not a public endpoint)
       ↓
Extract API key + tenant from URL
       ↓
validateApiKey(tenantId, key) → cryptoService.matches()
       ↓
If valid → ApiKeyAuthenticationToken(ROLE_API_USER)
       ↓
SecurityContext configured
       ↓
Checks hasAnyRole("ADMIN", "API_USER") → ✅ PASS
       ↓
Request authorized
```

### With JWT
```
Request with Authorization: Bearer
       ↓
JWT Filter (default Spring) processes first
       ↓
Validates token + extracts roles
       ↓
SecurityContext configured with JWT roles
       ↓
Checks endpoint-specific rule
       ↓
Request authorized (or denied)
```

## 🚨 Error Scenarios

### 401 Unauthorized
- Invalid API key
- Expired JWT
- Authentication failure

### 403 Forbidden
- Valid JWT but incorrect role
- Attempting to access endpoint without permission

### Error Examples
```bash
# Invalid API key → 401
curl -X GET http://localhost:8080/api/tenants/tenant1/data \
  -H "X-API-Key: invalid_key"

# JWT without ADMIN role trying to manage keys → 403
curl -X POST http://localhost:8080/api/tenants/tenant1/api-keys \
  -H "Authorization: Bearer <jwt_user_token>"
```

## 📝 Best Practices

### ✅ Do's
- Use API keys for service-to-service communication
- Use JWT for administrative operations
- Keep API keys secure (not in code)
- Implement regular API key rotation

### ❌ Don'ts
- Don't expose API keys in frontend
- Don't use API keys for administrative operations
- Don't share API keys between tenants
- Don't ignore security logs

## 🔧 Debug and Monitoring

### Security Logs
```properties
# To enable detailed logs
logging.level.com.lingotlow.backendcore.infrastructure.security=DEBUG
logging.level.org.springframework.security=DEBUG
```

### What to Monitor
- Invalid API key attempts
- Users trying to access endpoints without permission
- Suspicious usage patterns
- Key rotation

---

**Important:** This system ensures API keys can only be used for data access, never for managing other API keys, maintaining the principle of least privilege.
