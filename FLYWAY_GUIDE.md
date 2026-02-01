# 🚀 Complete Flyway Guide - Lingotlow Backend

## 📋 **What is Flyway?**

Flyway is a **database versioning tool** that evolves your schema in a **controlled and traceable** way. Think of it as "Git for your database".

### 🎯 **Why do we use Flyway?**
- ✅ **Control**: Every change has a version number
- ✅ **Security**: No unexpected changes in production
- ✅ **Traceability**: Complete history of all changes
- ✅ **Rollback**: Ability to revert changes
- ✅ **Automation**: Integrated with application startup

---

## 🏗️ **How it Works in Lingotlow**

### **File Structure:**
```
src/main/resources/db/migration/
├── V1__Create_tenants_table.sql      # Creates tenants table
├── V2__Create_api_keys_table.sql      # Creates api_keys table
├── V3__Create_events_table.sql        # Creates events table
└── V4__Add_performance_indexes.sql    # Next migration...
```

### **Flyway Table (Automatically Created):**
```sql
-- flyway_schema_history
┌─────────┬──────────────┬─────────────────┬─────────────┐
│ version │ description │ installed_on    │ success     │
├─────────┼──────────────┼─────────────────┼─────────────┤
│ 1       │ Create       │ 2024-01-28...   │ true        │
│ 2       │ Create       │ 2024-01-28...   │ true        │
│ 3       │ Create       │ 2024-01-28...   │ true        │
└─────────┴──────────────┴─────────────────┴─────────────┘
```

---

## 🛠️ **Project Configuration**

### **application.yml (Production/Staging):**
```yaml
spring:
  flyway:
    enabled: true                    # ✅ Flyway ENABLED
    locations: classpath:db/migration
    baseline-on-migrate: true        # Creates baseline if not exists
    validate-on-migrate: true       # Validates before migrating
    
  jpa:
    hibernate:
      ddl-auto: validate            # ✅ Only validates, doesn't alter
    show-sql: false
```

### **application-local.yml (Local Development):**
```yaml
spring:
  flyway:
    enabled: true                    # ✅ Flyway ENABLED LOCALLY
    locations: classpath:db/migration
    baseline-on-migrate: true       # Creates baseline if not exist
    
  jpa:
    hibernate:
      ddl-auto: validate            # ✅ Only validates, doesn't alter
    show-sql: false
```

### **🎯 New Approach - Flyway in All Environments:**
- **Local**: Flyway enabled + `ddl-auto: validate`
- **Staging**: Flyway enabled + `ddl-auto: validate`  
- **Production**: Flyway enabled + `ddl-auto: validate`

**Benefits:**
- ✅ **Consistency**: Same behavior in all environments
- ✅ **Security**: No `ddl-auto: update` in any environment
- ✅ **Control**: Complete schema versioning
- ✅ **Preparation**: Local environment identical to production

---

## 🚀 **How to Use Day to Day**

### **1. Run Application Locally:**
```bash
# Uses application-local.yml (Flyway ENABLED)
./mvnw spring-boot:run -Dspring.profiles.active=local

# Or if you don't have Maven wrapper:
mvn spring-boot:run -Dspring.profiles.active=local
```

### **2. Run in Production/Staging:**
```bash
# Uses application.yml (Flyway ENABLED)
./mvnw spring-boot: run

# Or explicitly:
./mvnw spring-boot:run -Dspring.profiles.active=default
```

### **3. Direct Flyway Commands:**
```bash
# Check migration status
./mvnw flyway:info

# Execute pending migrations
./mvnw flyway:migrate

# Clean database (careful!)
./mvnw flyway:clean

# Validate migrations
./mvnw flyway:validate

# Repair schema history
./mvnw flyway:repair
```

### **🎯 Unified Workflow:**
- **Environments**: All use Flyway + `ddl-auto: validate`
- **Consistency**: Identical behavior in all environments
- **Security**: No environment uses `ddl-auto: update`
- **Result**: Local environment is exact replica of production

---

## 🗄️ **Local Database Connection**

### **Local PostgreSQL Configuration:**
```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lingotlow_dev
    username: lingotlow
    password: lingotlow_dev
    driver-class-name: org.postgresql.Driver
```

### **How to Connect:**

#### **Via psql (Terminal):**
```bash
# Connect to database
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev

# Password: lingotlow_dev
```

#### **Via DBeaver/PGAdmin:**
```
Host: localhost
Port: 5432
Database: lingotlow_dev
User: lingotlow
Password: lingotlow_dev
```

#### **Docker PostgreSQL (If you don't have local):**
```bash
# Start PostgreSQL
docker run --name postgres-lingotlow \
  -e POSTGRES_DB=lingotlow_dev \
  -e POSTGRES_USER=lingotlow \
  -e POSTGRES_PASSWORD=lingotlow_dev \
  -p 5432:5432 \
  -d postgres:15-alpine

# Connect
docker exec -it postgres-lingotlow psql -U lingotlow -d lingotlow_dev
```

---

## 🧪 **Tests and Debug**

### **1. Check Current Schema:**
```sql
-- Check created tables
\dt

-- Check events table structure
\d events

-- Check indexes
\di

-- Check flyway table
SELECT * FROM flyway_schema_history ORDER BY installed_on;
```

### **2. Test Queries:**
```sql
-- Test insertion in events table
INSERT INTO events (tenant_id, request_id, document_id, type, status, source_ip)
VALUES (
    gen_random_uuid(),
    gen_random_uuid(),
    'test-doc-123',
    'test.event',
    'RECEIVED',
    '127.0.0.1'
);

-- Check insertion
SELECT * FROM events WHERE document_id = 'test-doc-123';
```

### **3. Debug Flyway:**
```yaml
# application.yml (add for debug)
logging:
  level:
    org.flywaydb: DEBUG
    org.springframework.boot.autoconfigure.flyway: DEBUG
```

---

## 📝 **Creating New Migrations**

### **Naming Rules:**
- `V` + number + `__` + description
- Example: `V4__Add_performance_indexes.sql`
- Numbers must be sequential

### **Migration Template:**
```sql
-- V4__Add_performance_indexes.sql
-- Migration: Add performance indexes for events table
-- Author: Your Name
-- Date: 2024-01-28

-- Add composite index for tenant + status queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_tenant_status_composite 
ON events(tenant_id, status, created_at);

-- Add index for document_id lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_document_id 
ON events(document_id) WHERE document_id IS NOT NULL;

-- Add comment for documentation
COMMENT ON INDEX idx_events_tenant_status_composite IS 'Optimizes tenant status queries';
```

### **Best Practices:**
```sql
-- ✅ Use IF NOT EXISTS to be idempotent
CREATE TABLE IF NOT EXISTS new_table (...);

-- ✅ Use CONCURRENTLY to avoid blocking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_name ON table(column);

-- ✅ Add comments
COMMENT ON TABLE events IS 'Stores event metadata and processing status';

-- ✅ Check before dropping
DROP TABLE IF EXISTS old_table CASCADE;
```

---

## 🔧 **Useful Scripts**

### **Local Reset Script:**
```bash
#!/bin/bash
# reset-local-db.sh

echo "🗑️  Resetting local database..."

# Connect and clean
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev << EOF
-- Clean tables (except flyway_schema_history)
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS api_keys CASCADE;
DROP TABLE IF EXISTS tenants CASCADE;

-- Clean flyway history
DELETE FROM flyway_schema_history;

EOF

echo "✅ Database reset! Run application to recreate."
```

### **Backup Script:**
```bash
#!/bin/bash
# backup-local-db.sh

BACKUP_FILE="lingotlow_backup_$(date +%Y%m%d_%H%M%S).sql"

echo "💾 Creating backup: $BACKUP_FILE"

pg_dump -h localhost -p 5432 -U lingotlow -d lingotlow_dev > "$BACKUP_FILE"

echo "✅ Backup created: $BACKUP_FILE"
```

### **Verification Script:**
```bash
#!/bin/bash
# check-db-status.sh

echo "🔍 Checking database status..."

echo "📊 Tables:"
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev -c "\dt"

echo "📈 Flyway Migrations:"
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_on;"

echo "🎯 Application status:"
curl -s http://localhost:8080/actuator/health | jq .
```

---

## 🚨 **Common Troubleshooting**

### **Problem 1: Migration Failed**
```bash
# Check error
./mvnw flyway:info

# Check detailed log
./mvnw spring-boot:run -Dspring.profiles.active=default -Dlogging.level.org.flywaydb=DEBUG

# Repair (if necessary)
./mvnw flyway:repair
```

### **Problem 2: Schema Outdated**
```bash
# Force validation
./mvnw flyway:validate

# If fails, clean and recreate
./mvnw flyway:clean
./mvnw flyway:migrate
```

### **Problem 3: Database Connection Failed**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check if port is available
netstat -tlnp | grep 5432

# Test direct connection
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev -c "SELECT 1;"
```

---

## 📊 **Monitoring**

### **Useful Endpoints:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Flyway info (if exposed)
curl http://localhost:8080/actuator/flyway

# Metrics
curl http://localhost:8080/actuator/metrics
```

### **Monitoring Queries:**
```sql
-- Check locks
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS current_statement_in_blocking_process
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- Check table sizes
SELECT schemaname,tablename,attname,n_distinct,correlation 
FROM pg_stats 
WHERE tablename = 'events';
```

---

## 🎯 **Professional Tips**

### **1. Local Development Environment:**
- Use `ddl-auto: validate` for consistency
- Test migrations before committing
- Keep a clean database
- **Advantage**: Local environment identical to production

### **2. Staging Environment:**
- Use `ddl-auto: validate` + Flyway
- Test migrations with real data
- Check performance
- **Consistency**: Same behavior as local dev

### **3. Production:**
- Use `ddl-auto: validate` + Flyway
- Always have backup before
- Monitor migration time
- **Security**: No surprises, known behavior

### **4. Collaboration:**
- Commit migrations separately from code
- Use PRs to review SQL
- Document complex changes
- **Benefit**: Everyone works with same schema

### **🌟 New Reality - Total Consistency:**
- ✅ **Local Dev** = **Staging** = **Production**
- ✅ Same migrations in all environments
- ✅ Same JPA/Flyway behavior
- ✅ Zero surprises on deploy

---

## 📚 **Quick References**

### **Flyway Commands:**
| Command | Description |
|---------|-----------|
| `flyway:info` | Shows migration status |
| `flyway:migrate` | Executes pending migrations |
| `flyway:validate` | Validates migrations |
| `flyway:clean` | Removes all objects |
| `flyway:repair` | Repairs schema history |

### **Spring Configurations (New Approach):**
| Property | Value (All Environments) | Description |
|-------------|------------------------|-----------|
| `spring.flyway.enabled` | `true` | Flyway always enabled |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Only validates, doesn't alter |
| `spring.flyway.baseline-on-migrate` | `true` | Creates baseline automatically |

### **🔄 Old vs New Configurations:**
| Environment | Old | New |
|----------|--------|------|
| **Local** | Flyway: `false`, DDL: `update` | Flyway: `true`, DDL: `validate` |
| **Staging** | Flyway: `true`, DDL: `validate` | Flyway: `true`, DDL: `validate` |
| **Production** | Flyway: `true`, DDL: `validate` | Flyway: `true`, DDL: `validate` |

---

**🎉 Congratulations!** Now you have complete control over your database schema with Flyway! 🚀
