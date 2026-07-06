# Todo API

REST API and single-page UI for managing tasks — Spring Boot 3.5, PostgreSQL, Redis.

## Architecture

The service uses the **cache-aside** pattern via Spring Cache:

- On **read**, Spring AOP checks Redis first. On a cache miss the method runs, logs `CACHE MISS`, queries PostgreSQL, then populates the cache (TTL 10 min).
- On **write** (create / update / delete), both the list entry (`tasks::all`) and the individual entry (`task::{id}`) are evicted so the next read is always consistent.
- Two cache regions: `tasks` (full sorted list) and `task` (individual item by id).
- Redis is optional at runtime — if unavailable the app continues to serve requests directly from PostgreSQL. The `/actuator/health` endpoint is unaffected by Redis status.

```
Browser / ALB
      │
      ▼
TaskController  ──────►  TaskService
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
               Redis cache          PostgreSQL
               (cache hit)          (cache miss → repopulate)
```

## Local Development

### Prerequisites

- Docker and Docker Compose
- Java 21 + Maven 3.9 (or `./mvnw`)

### Start infrastructure

```sh
docker compose up -d
```

Wait until both services are healthy:

```sh
docker compose ps
```

### Run the application

```sh
./mvnw spring-boot:run
```

API: `http://localhost:8080/api/tasks`  
UI:  `http://localhost:8080`

### Verify caching

```sh
# First call — one "CACHE MISS" line appears in the app log
curl -s http://localhost:8080/api/tasks

# Second call — no CACHE MISS; response served from Redis
curl -s http://localhost:8080/api/tasks
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/todo` | JDBC URL (use RDS Proxy endpoint on ECS) |
| `SPRING_DATASOURCE_USERNAME` | `todo` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `todo` | Database password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis hostname (ElastiCache primary endpoint on ECS) |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_DATA_REDIS_SSL` | `false` | Set `true` for ElastiCache in-transit encryption (required when TLS is enabled — plain connections hang silently) |

## API Endpoints

### List all tasks
```sh
curl -s http://localhost:8080/api/tasks | jq
```

### Get a task
```sh
curl -s http://localhost:8080/api/tasks/1 | jq
```

### Create a task
```sh
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Semi-skimmed"}' | jq
```

### Update a task
```sh
curl -s -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Semi-skimmed","completed":true}' | jq
```

### Delete a task
```sh
curl -s -X DELETE http://localhost:8080/api/tasks/1 -o /dev/null -w "%{http_code}\n"
```

### Health check (ALB target group path)
```sh
curl -s http://localhost:8080/actuator/health | jq
```

## Error Shape

All error responses use a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task not found",
  "path": "/api/tasks/99",
  "timestamp": "2026-07-06T10:00:00"
}
```

Validation failures (400) include the offending field: `"title: must not be blank"`.

## Docker Build

```sh
docker build -t todo-api .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/todo \
  -e SPRING_DATASOURCE_USERNAME=todo \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  -e SPRING_DATA_REDIS_HOST=redis-host \
  -e SPRING_DATA_REDIS_SSL=true \
  todo-api
```
