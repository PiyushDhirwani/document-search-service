# Distributed Document Search Service

A prototype of a distributed document search service built with **Spring Boot 3.4.5** and **Java 21**, capable of searching through documents with sub-second response times. Demonstrates enterprise-grade architectural patterns including multi-tenancy, fault tolerance, caching, and horizontal scalability.

---

## Architecture Overview

```
┌─────────────┐     ┌──────────────────────────────────┐     ┌─────────────┐
│   Client     │────▶│   Spring Boot Application        │────▶│   MySQL 8   │
│  (REST API)  │     │                                  │     │  (Primary)  │
└─────────────┘     │  ┌───────────┐ ┌──────────────┐  │     └─────────────┘
                    │  │Rate Limit │ │Circuit Breaker│  │
                    │  │Interceptor│ │ (Resilience4j)│  │     ┌─────────────┐
                    │  └───────────┘ └──────────────┘  │────▶│   Redis      │
                    │                                  │     │  (Cache)     │
                    │  ┌───────────────────────────┐   │     └─────────────┘
                    │  │  Search Strategy           │   │
                    │  │  1. Elasticsearch (if on)  │   │     ┌─────────────┐
                    │  │  2. MySQL Full-Text Search │   │────▶│Elasticsearch│
                    │  │  3. MySQL LIKE (fallback)  │   │     │ (Optional)  │
                    │  └───────────────────────────┘   │     └─────────────┘
                    └──────────────────────────────────┘
```

### Search Strategy (Tiered Fallback)
1. **Elasticsearch** — Fuzzy matching, relevance ranking, field boosting (`title^3`, `tags^2`). Enabled via config.
2. **MySQL Full-Text Search** — `MATCH ... AGAINST` in natural language mode with relevance scoring. Default.
3. **MySQL LIKE** — Fallback if full-text index is unavailable.

---

## Tech Stack

| Component           | Technology                          |
|---------------------|-------------------------------------|
| Framework           | Spring Boot 3.4.5                   |
| Language            | Java 21                             |
| Primary Database    | MySQL 8 (with Full-Text Search)     |
| Search Engine       | Elasticsearch 8.13 (optional)       |
| Caching             | Redis                               |
| Fault Tolerance     | Resilience4j Circuit Breaker        |
| API Documentation   | SpringDoc OpenAPI (Swagger UI)      |
| Build Tool          | Maven                               |

---

## Prerequisites

- **Java 21+**
- **MySQL 8** running on `localhost:3306`
- **Redis** running on `localhost:6379`
- **Docker** (optional, for Elasticsearch)

---

## Quick Start

### 1. Clone and Build
```bash
git clone <repository-url>
cd document-search-service
./mvnw clean install
```

### 2. Start Dependencies
```bash
# Start MySQL (if not running)
mysql.server start

# Start Redis
redis-server

# (Optional) Start Elasticsearch via Docker
docker-compose up -d
```

### 3. Create the Database
MySQL database `document_service` is auto-created on startup. For optimal search, create the full-text index:
```sql
CREATE FULLTEXT INDEX idx_fulltext_search ON documents(title, content);
```

### 4. Run the Application
```bash
./mvnw spring-boot:run
```
The service starts on **http://localhost:8081**

### 5. Access Swagger UI
Open **http://localhost:8081/swagger-ui/index.html** for interactive API docs.

---

## API Endpoints

### Create a Document
```bash
curl -X POST http://localhost:8081/api/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{
    "title": "Introduction to Distributed Systems",
    "content": "Distributed systems are collections of independent computers...",
    "author": "John Doe",
    "tags": ["distributed", "systems", "architecture"]
  }'
```

### Search Documents
```bash
curl "http://localhost:8081/api/search?q=distributed+systems&page=0&size=20" \
  -H "X-Tenant-Id: tenant-1"
```

### Get Document by ID
```bash
curl http://localhost:8081/api/documents/{id} \
  -H "X-Tenant-Id: tenant-1"
```

### Delete Document (Soft Delete)
```bash
curl -X DELETE http://localhost:8081/api/documents/{id} \
  -H "X-Tenant-Id: tenant-1"
```

### Health Check
```bash
curl http://localhost:8081/health
```

---

## Key Features

### Multi-Tenancy
- Tenant isolation via `X-Tenant-Id` request header (required on all `/api/**` endpoints)
- All queries are scoped to the tenant — tenants cannot access each other's documents

### Caching (Redis)
- **Document cache**: 10-minute TTL, keyed by `tenantId:documentId`
- **Search cache**: 2-minute TTL
- **Graceful degradation**: App continues working if Redis is down (via `CacheErrorHandler`)

### Rate Limiting
- Per-tenant rate limiting: **100 requests/minute** (configurable)
- Returns `429 Too Many Requests` when limit is exceeded
- In-memory counters with automatic window reset

### Circuit Breaker (Resilience4j)
- Sliding window size: 10 requests
- Failure threshold: 50%
- Open state duration: 10 seconds
- Fallback methods return graceful error responses

### Elasticsearch Integration (Optional)
- **Dual-write**: Documents are persisted to MySQL and indexed in Elasticsearch
- **Feature toggle**: Controlled by `app.elasticsearch.enabled` property
- **Fuzzy search**: Auto-fuzziness with `multi_match` across title, content, tags, metadata
- **Field boosting**: Title (3x), Tags (2x) for better relevance ranking
- Falls back to MySQL when ES is unavailable

---

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | Application port |
| `app.elasticsearch.enabled` | `false` | Enable/disable Elasticsearch |
| `app.rate-limit.requests-per-minute` | `100` | Rate limit per tenant |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/document_service` | MySQL connection |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch URL |

### Enabling Elasticsearch
1. Start Elasticsearch: `docker-compose up -d`
2. Set `app.elasticsearch.enabled=true` in `application.properties`
3. Remove the `spring.autoconfigure.exclude` line
4. Restart the application

---

## Project Structure

```
src/main/java/com/distributed_document/document_search_service/
├── DocumentSearchServiceApplication.java   # Main application entry point
├── config/
│   ├── ElasticsearchConfig.java            # ES repository config (conditional)
│   ├── RateLimitingInterceptor.java        # Per-tenant rate limiting
│   ├── RedisConfig.java                    # Redis cache manager + error handler
│   └── WebConfig.java                      # Registers interceptors
├── controller/
│   ├── DocumentController.java             # REST API endpoints
│   └── HealthController.java               # Health check endpoint
├── dto/
│   ├── DocumentRequest.java                # Input DTO with validation
│   ├── DocumentResponse.java               # Output DTO
│   └── SearchResponse.java                 # Search results wrapper
├── exception/
│   ├── DocumentNotFoundException.java      # 404 exception
│   ├── GlobalExceptionHandler.java         # Centralized error handling
│   └── RateLimitExceededException.java     # 429 exception
├── model/
│   ├── Document.java                       # JPA entity (MySQL)
│   └── DocumentIndex.java                  # ES document mapping
├── repository/
│   ├── DocumentRepository.java             # JPA repository (MySQL)
│   └── es/
│       └── DocumentSearchRepository.java   # ES repository
└── service/
    ├── DocumentService.java                # Core business logic
    └── ElasticsearchService.java           # ES operations (conditional)
```

---

## Scalability Considerations

- **Horizontal scaling**: Stateless application design allows running multiple instances behind a load balancer
- **Database sharding**: Tenant-based partitioning strategy ready via `tenantId` column
- **Cache layer**: Redis reduces database load for repeated queries
- **Search offloading**: Elasticsearch handles search traffic independently from the primary database
- **Circuit breakers**: Prevent cascading failures across service dependencies

---

## Docker Support

A `docker-compose.yml` is provided for running Elasticsearch:
```bash
docker-compose up -d    # Start ES
docker-compose down     # Stop ES
```

---

## References

- **Windsurf IDE** and **Claude AI** were used as references for code development, documentation, and learning purposes.

---

## License

This project is a prototype for demonstration purposes.