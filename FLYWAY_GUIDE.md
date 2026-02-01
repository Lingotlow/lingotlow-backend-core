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

## 🏗️ **Architecture Overview: PostgreSQL + JPA + Flyway + Redis**

### 📊 **Data Layer Architecture**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Layer     │    │  Business Logic  │    │  Data Layer     │
│                 │    │                  │    │                 │
│ REST Endpoints  │───▶│   EventService   │───▶│ PostgreSQL      │
│                 │    │   TenantService  │    │ (Primary Store) │
│ Validation      │    │   ApiKeyService  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Queue Layer    │    │  Processing      │    │  Schema Layer   │
│                 │    │                  │    │                 │
│ Redis Streams   │◀───│ QueueFailure     │    │ Flyway          │
│ (Upstash)       │    │ Handler          │    │ (Versioning)    │
│ Async Processing│    │ Retry Logic      │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### 🎯 **Purpose of Each Component**

#### **📊 PostgreSQL + JPA (Primary Data Store)**
- **PostgreSQL**: Banco de dados principal onde os eventos são **persistidos permanentemente**
- **JPA/Hibernate**: ORM para mapear objetos Java para tabelas do PostgreSQL
- **Purpose**: 
  - ✅ **Durabilidade**: Eventos nunca perdem
  - ✅ **Consistência**: ACID transactions
  - ✅ **Queries**: Consultas complexas, relatórios
  - ✅ **Auditoria**: Histórico completo

#### **🔄 Flyway (Schema Management)**
- **Purpose**: Controle de versão do schema do banco (migrations)
- **Benefits**:
  - ✅ **Version Control**: Cada mudança tem versão
  - ✅ **Safety**: Sem mudanças inesperadas
  - ✅ **Consistency**: Mesmo schema em todos ambientes
  - ✅ **Rollback**: Capacidade de reverter mudanças

#### **🚀 Redis Streams (Async Processing)**
- **Redis Streams**: Fila de mensagens para **processamento assíncrono**
- **Upstash**: Serviço gerenciado de Redis na nuvem
- **Purpose**:
  - ✅ **Performance**: Não bloqueia API
  - ✅ **Escalabilidade**: Processa milhares de eventos/segundo
  - ✅ **Desacoplamento**: API independe do processamento
  - ✅ **Retry**: Reprocessamento automático em falhas

### 📋 **Complete Data Flow**

```
1. API recebe evento
   ↓
2. JPA salva no PostgreSQL (garantia de persistência)
   ↓
3. Flyway garante schema consistente
   ↓
4. Redis Streams enfileira para processamento async
   ↓
5. Consumer processa da fila (analytics, notificações, etc)
```

### 🎖️ **Benefits of This Architecture**

1. **Resiliência**: Se fila falhar, evento ainda está salvo no PostgreSQL
2. **Performance**: API responde rápido (200ms vs 2s)
3. **Escalabilidade**: Processamento independente do armazenamento
4. **Monitoramento**: Métricas de ambos os lados (banco + fila)
5. **Flexibilidade**: Múltiples consumers da mesma fila
6. **Consistência**: Schema controlado por Flyway em todos ambientes

### 🔄 **Why All Three Components?**

#### **Without Redis Streams:**
```
POST /events → Salva no banco → Processa analytics → Responde (2s)
```

#### **With Redis Streams:**
```
POST /events → Salva no banco → Enfileira → Responde (200ms)
                                    ↓
                              Consumer processa analytics (background)
```

#### **Without Flyway:**
- Schema descontrolado
- Mudanças manuais e arriscadas
- Diferenças entre ambientes
- Impossível rollback

#### **With Flyway:**
- Schema versionado e controlado
- Mudanças seguras e rastreáveis
- Consistência total entre ambientes
- Rollback seguro quando necessário

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

### **📚 Component Integration Summary**

#### **🎯 How They Work Together:**

1. **PostgreSQL + JPA**:
   - Armazena dados permanentemente
   - Garante ACID transactions
   - Permite queries complexas

2. **Flyway**:
   - Controla evolução do schema
   - Garante consistência entre ambientes
   - Permite rollback seguro

3. **Redis Streams**:
   - Processa eventos assincronamente
   - Não bloqueia a API
   - Permite escalabilidade horizontal

#### **🔧 Configuration Integration:**
```yaml
spring:
  # Database (PostgreSQL + JPA)
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/lingotlow_dev}
    username: ${DATABASE_USERNAME:lingotlow}
    password: ${DATABASE_PASSWORD:lingotlow_dev}
    driver-class-name: org.postgresql.Driver
  
  # Schema Management (Flyway)
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  
  # ORM (JPA/Hibernate)
  jpa:
    hibernate:
      ddl-auto: validate  # Only validates, doesn't alter
  
  # Async Processing (Redis)
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

---

## 🔌 **Understanding Datasource Configuration**

### **O que é Datasource?**

**Datasource** é a **configuração de conexão com o banco de dados** no Spring Boot! É o objeto que gerencia:

- **Conexão** com o banco de dados
- **Pool de conexões** (reaproveitamento)
- **Credenciais** (usuário, senha, URL)
- **Configurações** (timeout, driver, etc.)

### **🏗️ Como funciona na arquitetura:**

```
┌─────────────────┐
│   Spring Boot   │
│                 │
│ ┌─────────────┐ │
│ │ Datasource  │ │ ← Configura conexão com PostgreSQL
│ │ (Pool)      │ │
│ └─────────────┘ │
│        │        │
│        ▼        │
│ ┌─────────────┐ │
│ │ PostgreSQL  │ │ ← Banco de dados real
│ │ Server      │ │
│ └─────────────┘ │
└─────────────────┘
```

### **🎯 Para que serve cada parte do Datasource:**

#### **`url`**
- **Endereço completo** do banco de dados
- **Formato**: `jdbc:postgresql://host:port/database`
- **Exemplo**: `jdbc:postgresql://localhost:5432/lingotlow_dev`

#### **`username` & `password`**
- **Credenciais de acesso** ao PostgreSQL
- **Produção**: Usa variáveis de ambiente por segurança
- **Desenvolvimento**: Pode usar credenciais locais

#### **`driver-class-name`**
- **Classe JDBC** que sabe "conversar" com PostgreSQL
- `org.postgresql.Driver` = "Tradutor" Java ↔ PostgreSQL
- **Spring Boot**: Detecta automaticamente na maioria dos casos

### **🔄 Integração do Datasource com os Componentes:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   JPA/Hibernate │    │    Flyway   │    │ Repositories │     │
│  │             │    │             │    │             │     │
│  │ EventEntity │    │ Migrations  │    │ EventRepo   │     │
│  │ TenantEntity│    │ V1__Create  │    │ TenantRepo  │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│         │                   │                   │           │
│         └───────────────────┼───────────────────┘           │
│                             │                               │
│                    ┌─────────────┐                         │
│                    │ Datasource   │ ← Gerencia todas as     │
│                    │ (Pool)       │   conexões              │
│                    └─────────────┘                         │
│                             │                               │
│                             ▼                               │
│                    ┌─────────────┐                         │
│                    │ PostgreSQL  │ ← Banco de dados        │
│                    │ Database    │   persistente            │
│                    └─────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### **📋 Exemplo Prático de Uso:**

#### **Sem Datasource configurado:**
```bash
❌ Application startup failed
❌ "Could not determine a suitable DataSource"
❌ "No database connection available"
```

#### **Com Datasource configurado:**
```bash
✅ Starting application...
✅ HikariPool-1 - Starting...
✅ HikariPool-1 - Start completed.
✅ Database connection established!
✅ JPA entities can be saved
✅ Flyway migrations can run
✅ Everything working! 🚀
```

### **🔧 Configurações Avançadas do Datasource:**

```yaml
spring:
  datasource:
    # Básico
    url: jdbc:postgresql://localhost:5432/lingotlow_dev
    username: ${DATABASE_USERNAME:lingotlow}
    password: ${DATABASE_PASSWORD:lingotlow_dev}
    driver-class-name: org.postgresql.Driver
    
    # Pool de Conexões (HikariCP - padrão Spring Boot)
    hikari:
      maximum-pool-size: 20          # Máximo de conexões
      minimum-idle: 5                # Mínimo de conexões ociosas
      idle-timeout: 30000           # Tempo ocioso antes de fechar (ms)
      max-lifetime: 1800000         # Tempo máximo de vida da conexão (ms)
      connection-timeout: 20000     # Timeout para obter conexão (ms)
      leak-detection-threshold: 60000 # Detecta connection leaks
      
    # Validação
    validation-timeout: 3000         # Timeout para validação
    connection-test-query: "SELECT 1" # Query para testar conexão
```

### **🎖️ Benefícios do Pool de Conexões:**

| Benefício | Descrição |
|-----------|-----------|
| **Performance** | Reaproveita conexões existentes |
| **Escalabilidade** | Limita número máximo de conexões |
| **Resiliência** | Reconecta automaticamente em falhas |
| **Monitoramento** | Métricas de uso do pool |
| **Timeouts** | Evita esperas infinitas |

### **🚀 Boas Práticas:**

#### **1. Variáveis de Ambiente (Produção):**
```bash
export DATABASE_URL="jdbc://prod-db:5432/lingotlow_prod"
export DATABASE_USERNAME="app_user"
export DATABASE_PASSWORD="secure_password"
```

#### **2. Configuração por Ambiente:**
```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lingotlow_dev
    username: lingotlow
    password: lingotlow_dev

# application-prod.yml  
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

#### **3. Monitoramento:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  metrics:
    enable:
      hikaricp: true  # Métricas do pool de conexões
```

### **🔍 Troubleshooting Comum:**

#### **Problema: Connection Refused**
```bash
# Verifique se PostgreSQL está rodando
docker ps | grep postgres

# Verifique se porta está disponível
netstat -tlnp | grep 5432

# Teste conexão direta
psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev
```

#### **Problema: Pool Exhausted**
```yaml
# Aumente o pool se necessário
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

**🎉 O Datasource é a "ponte" fundamental entre sua aplicação Java e o banco PostgreSQL!** 🌉

#### **🚀 Benefits Summary:**

| Layer | Technology | Primary Benefit |
|-------|------------|-----------------|
| **Storage** | PostgreSQL + JPA | Data Durability |
| **Schema** | Flyway | Version Control |
| **Processing** | Redis Streams | Performance |

**🎉 Complete Enterprise Architecture!** 🚀

**🎉 Congratulations!** Now you have complete control over your database schema with Flyway! 🚀
