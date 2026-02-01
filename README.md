# Lingotlow Backend Core

## Overview

This Lingotlow backend-core project uses Java 21 + Spring Boot and depends on external services to run locally, which we start with Docker Compose.

## Dependencies Stack

- PostgreSQL 16
- Redis 7
- PgAdmin 4 (interface to manage PostgreSQL)
- Custom Docker network (lingotlow-network)

The Spring Boot backend will be executed locally via IntelliJ.

---

## Prerequisites for Mac and Linux (LMDE 6 - Debian)

### 1. Install Docker and Docker Compose

### On Linux LMDE 6 (Debian based):
##### Update packages
    sudo apt update && sudo apt upgrade -y

##### Install necessary packages to add repositories
    sudo apt install -y ca-certificates curl gnupg lsb-release

##### Create key for official Docker repository
    sudo mkdir -p /etc/apt/keyrings

##### Add Docker GPG key
    curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

##### Add Docker repository
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

##### Install Docker
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

##### Start and enable Docker
    sudo systemctl start docker
    sudo systemctl enable docker

##### Add user to docker group (to avoid sudo)
    sudo usermod -aG docker $USER

##### Apply group changes (logout and login or run)
    newgrp docker

### On Mac:
##### Install Docker Desktop
    brew install --cask docker

---

## Quick Start

### 1. Start Dependencies
```bash
# Start PostgreSQL, Redis, and PgAdmin
docker-compose up -d

# Check services status
docker-compose ps
```

### 2. Database Setup
```bash
# Access PostgreSQL
docker exec -it lingotlow-postgres psql -U lingotlow -d lingotlow_dev

# Or use PgAdmin at http://localhost:5050
# Server: postgres
# Username: lingotlow
# Password: lingotlow_dev
```

### 3. Run Application
```bash
# Using Maven
./mvnw spring-boot:run -Dspring.profiles.active=local

# Or using IntelliJ IDE
# - Open the project
# - Run BackendCoreApplication.java
```

### 4. Access Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# API Documentation
open http://localhost:8080/v3/api-docs
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/lingotlow/backendcore/
│   │   ├── domain/           # Domain layer (DDD)
│   │   │   ├── tenant/      # Tenant domain logic
│   │   │   ├── event/       # Event domain logic
│   │   │   └── apikey/      # API Key domain logic
│   │   ├── infrastructure/   # Infrastructure layer
│   │   │   ├── repository/  # JPA repositories
│   │   │   ├── logging/     # Audit logging
│   │   │   └── metrics/     # Metrics collection
│   │   └── interfaces/      # Interface layer
│   │       └── api/         # REST controllers
│   └── resources/
│       ├── application.yml          # Main configuration
│       ├── application-local.yml    # Local development
│       └── db/migration/            # Flyway migrations
└── test/                           # Test suite
    ├── java/                       # Unit and integration tests
    └── resources/                  # Test configurations
```

---

## Configuration

### Application Profiles

- **local**: Local development with Docker services
- **default**: Production/staging configuration

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lingotlow_dev
    username: lingotlow
    password: lingotlow_dev
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  
  jpa:
    hibernate:
      ddl-auto: validate
```

### Redis Configuration
```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

---

## API Endpoints

### Tenant Management
- `GET /api/tenants` - List all tenants
- `POST /api/tenants` - Create new tenant
- `GET /api/tenants/{tenantKey}` - Get tenant details
- `PUT /api/tenants/{tenantKey}` - Update tenant
- `DELETE /api/tenants/{tenantKey}` - Delete tenant

### API Key Management
- `POST /api/api-keys/{tenantKey}` - Create API key
- `GET /api/api-keys/{tenantKey}` - List API keys

### Event Ingestion
- `POST /api/ingest/{tenantKey}` - Ingest event

### Health & Monitoring
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

---

## Development

### Code Style
- Java 21 features
- Domain-Driven Design (DDD)
- Clean Architecture principles
- SOLID principles

### Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=EventServiceTest

# Run integration tests
./mvnw test -Dtest=EventControllerIntegrationTest
```

### Database Migrations
```bash
# Check migration status
./mvnw flyway:info

# Run pending migrations
./mvnw flyway:migrate

# Clean database (development only)
./mvnw flyway:clean
```

---

## Environment Variables

### Optional Environment Variables
```bash
# Override default configuration
export SPRING_PROFILES_ACTIVE=local
export SERVER_PORT=8080
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=lingotlow_dev
export DB_USER=lingotlow
export DB_PASSWORD=lingotlow_dev
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

---

## Troubleshooting

### Common Issues

#### Docker Permission Denied
```bash
# Fix Docker permissions
sudo usermod -aG docker $USER
newgrp docker
```

#### Port Already in Use
```bash
# Check what's using port 5432
lsof -i :5432

# Kill process if needed
kill -9 <PID>
```

#### Database Connection Failed
```bash
# Check PostgreSQL container
docker logs lingotlow-postgres

# Restart services
docker-compose restart
```

#### Application Won't Start
```bash
# Check application logs
./mvnw spring-boot:run -Dspring.profiles.active=local -Dlogging.level.root=DEBUG

# Clean and rebuild
./mvnw clean install
```

---

## Monitoring & Observability

### Health Endpoints
- Application health: `/actuator/health`
- Database health: Included in main health check
- Redis health: Included in main health check

### Metrics
- JVM metrics: `/actuator/metrics/jvm.*`
- HTTP metrics: `/actuator/metrics/http.server.requests`
- Database metrics: `/actuator/metrics/data.*`

### Logging
- Application logs: Console output
- Audit logs: Structured JSON format
- Database logs: Docker container logs

---

## Security

### Authentication
- API Key based authentication
- JWT tokens for internal services
- Tenant-based access control

### Data Protection
- Passwords encrypted in database
- API keys hashed
- HTTPS in production

---

## Performance

### Database Optimization
- Indexed columns for frequent queries
- Connection pooling configured
- Flyway migrations for schema management

### Caching
- Redis for session management
- Application-level caching for frequent data

### Monitoring
- Response time tracking
- Error rate monitoring
- Resource usage metrics

---

## Contributing

1. Fork the repository
2. Create feature branch
3. Make your changes
4. Add tests
5. Run all tests
6. Submit pull request

### Code Review Checklist
- [ ] Code follows project style
- [ ] Tests are included
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Security considerations addressed

---

## License

This project is proprietary software of Lingotlow.

---

## Support

For technical support:
- Create issue in repository
- Contact development team
- Check documentation first

---

**Happy Coding! 🚀**
