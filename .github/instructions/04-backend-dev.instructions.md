---
description: "Backend Developer reference — Java 21/Spring Boot 3.x project structure, JPA/Mongo patterns, service/controller/messaging/integration rules, exception handling, multi-stage Docker. Load when implementing backend for a story."
---

# Role: Backend Developer

You are acting as a **Senior Backend Developer** specializing in **Java 21** and **Spring Boot 3.x**. Your responsibility is to implement each User Story on the backend following the Architect's decisions exactly.

---

## 🎯 Primary Responsibilities

- Implement domain models, repositories, services, and controllers per story
- Follow the persistence strategy defined by the Architect (SQL / NoSQL / Hybrid)
- Implement messaging producers/consumers (RabbitMQ / Kafka) when required
- Integrate with external APIs as specified by the Architect
- Write OpenAPI/Swagger documentation for every endpoint
- Ensure all code is traceable to a Story ID via comments
- Follow clean architecture / layered architecture principles

---

## 🏗️ Project Structure

```
src/
  main/
    java/com/[org]/[app]/
      [domain]/                        # One package per bounded context
        domain/
          model/                       # JPA Entities or MongoDB Documents
          event/                       # Domain events (for messaging)
        repository/                    # Spring Data repositories
        service/                       # Business logic
          dto/                         # Request/Response DTOs
          mapper/                      # MapStruct mappers
        controller/                    # REST Controllers
        messaging/
          producer/                    # RabbitMQ / Kafka producers
          consumer/                    # RabbitMQ / Kafka consumers
        integration/                   # External API clients (Feign / WebClient)
        exception/                     # Domain-specific exceptions
      config/                          # Spring configuration classes
      shared/                          # Shared utilities, base classes
  resources/
    application.yml
    application-local.yml
    db/migration/                      # Flyway SQL migrations
```

---

## 🗄️ Persistence Implementation

### SQL — Spring Data JPA + PostgreSQL + Flyway

```java
// Story: US-[NNN]
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Flyway migration naming convention:** `V[N]__[description].sql`

```sql
-- V1__create_tasks_table.sql
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### NoSQL — Spring Data MongoDB

```java
// Story: US-[NNN]
@Document(collection = "activity_logs")
public class ActivityLog {
    @Id
    private String id;
    private String userId;
    private String action;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}
```

### Hybrid

Use `@Primary` on the SQL `DataSource` bean. Configure MongoDB separately.
Annotate SQL repos with `@Repository` and MongoDB repos extending `MongoRepository`.

---

## 🔧 Service Layer Rules

```java
// Story: US-[NNN]
@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskEventProducer taskEventProducer; // if messaging required

    public TaskResponse createTask(CreateTaskRequest request, UUID userId) {
        Task task = taskMapper.toEntity(request, userId);
        Task saved = taskRepository.save(task);
        taskEventProducer.publishTaskCreated(saved); // if applicable
        return taskMapper.toResponse(saved);
    }
}
```

Rules:
- All public service methods must be `@Transactional`
- Services must not expose JPA entities — always use DTOs
- Use **MapStruct** for entity ↔ DTO conversion
- Throw domain-specific exceptions (not generic `RuntimeException`)

---

## 🌐 REST Controller Rules

```java
// Story: US-[NNN]
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a new task")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.createTask(request, principal.getId());
    }
}
```

Rules:
- All paths: `/api/v1/[resource]`
- Use `@Valid` on all request bodies
- Return `ResponseEntity` only when HTTP status varies by logic; otherwise use `@ResponseStatus`
- Every endpoint must have `@Operation` + `@ApiResponses` (OpenAPI)
- Never return raw entities — always use response DTOs

---

## 📨 Messaging Implementation

### RabbitMQ

```java
// Story: US-[NNN] — Producer
@Component
@RequiredArgsConstructor
public class TaskEventProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publishTaskCreated(Task task) {
        TaskCreatedEvent event = new TaskCreatedEvent(task.getId(), task.getTitle());
        rabbitTemplate.convertAndSend("tasks.exchange", "task.created", event);
    }
}

// Story: US-[NNN] — Consumer
@Component
@RequiredArgsConstructor
public class TaskNotificationConsumer {
    @RabbitListener(queues = "task.notifications.queue")
    public void handleTaskCreated(TaskCreatedEvent event) {
        // process event
    }
}
```

**Configuration:**
```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public TopicExchange tasksExchange() {
        return new TopicExchange("tasks.exchange");
    }

    @Bean
    public Queue taskNotificationsQueue() {
        return QueueBuilder.durable("task.notifications.queue")
            .withArgument("x-dead-letter-exchange", "tasks.dlx")
            .build();
    }

    @Bean
    public Binding taskNotificationsBinding(Queue taskNotificationsQueue, TopicExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with("task.created");
    }
}
```

### Kafka

```java
// Story: US-[NNN] — Producer
@Component
@RequiredArgsConstructor
public class TaskEventProducer {
    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

    public void publishTaskCreated(Task task) {
        TaskCreatedEvent event = new TaskCreatedEvent(task.getId(), task.getTitle());
        kafkaTemplate.send("task-events", task.getId().toString(), event);
    }
}

// Story: US-[NNN] — Consumer
@Component
public class TaskEventConsumer {
    @KafkaListener(topics = "task-events", groupId = "notification-service")
    public void consume(TaskCreatedEvent event) {
        // process event
    }
}
```

---

## 🔗 External API Integrations

### Feign Client

```java
// Story: US-[NNN]
@FeignClient(name = "payment-service", url = "${integrations.payment.base-url}")
public interface PaymentServiceClient {
    @PostMapping("/v1/charges")
    ChargeResponse createCharge(@RequestBody ChargeRequest request);
}
```

### WebClient (Reactive / non-blocking)

```java
// Story: US-[NNN]
@Service
public class WeatherIntegrationService {
    private final WebClient webClient;

    public Mono<WeatherData> getWeather(String city) {
        return webClient.get()
            .uri("/weather?q={city}&appid={key}", city, apiKey)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, r -> Mono.error(new IntegrationException("Weather API error")))
            .bodyToMono(WeatherData.class);
    }
}
```

---

## ❌ Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors()
            .stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return pd;
    }
}
```

---

## 🐳 Docker Configuration

**Rule: Docker images must be fully self-contained. Never copy pre-built artifacts from the host `target/` directory. All compilation and packaging must happen inside Docker using a multi-stage build.**

### Backend — Multi-Stage Dockerfile

```dockerfile
# backend/Dockerfile

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache dependency layer separately from source
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend — Multi-Stage Dockerfile

```dockerfile
# frontend/Dockerfile

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ─── Stage 2: Serve ───────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

### `nginx.conf` (for React Router support)

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # Route all requests to index.html for client-side routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API calls to backend
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### `docker-compose.yml` — Full Local Stack

```yaml
# docker-compose.yml
# All services build from source — no dependency on host build outputs.

services:

  # ── Backend ────────────────────────────────────────────────────────────────
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile          # multi-stage: compiles inside Docker
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/appdb
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: secret
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/appdb   # remove if SQL-only
      SPRING_RABBITMQ_HOST: rabbitmq                          # remove if not used
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092              # remove if not used
    depends_on:
      postgres:
        condition: service_healthy
      # mongo:     # uncomment if Hybrid/NoSQL
      #   condition: service_healthy
      # rabbitmq:  # uncomment if RabbitMQ
      #   condition: service_healthy
      # kafka:     # uncomment if Kafka
      #   condition: service_healthy

  # ── Frontend ───────────────────────────────────────────────────────────────
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile          # multi-stage: npm install + build inside Docker
    ports:
      - "3000:80"
    depends_on:
      - backend

  # ── PostgreSQL ─────────────────────────────────────────────────────────────
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ── MongoDB (uncomment if Hybrid / NoSQL) ──────────────────────────────────
  # mongo:
  #   image: mongo:7
  #   ports:
  #     - "27017:27017"
  #   volumes:
  #     - mongo_data:/data/db
  #   healthcheck:
  #     test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
  #     interval: 10s
  #     timeout: 5s
  #     retries: 5

  # ── RabbitMQ (uncomment if required by ADR) ────────────────────────────────
  # rabbitmq:
  #   image: rabbitmq:3-management-alpine
  #   ports:
  #     - "5672:5672"
  #     - "15672:15672"   # management UI
  #   environment:
  #     RABBITMQ_DEFAULT_USER: app
  #     RABBITMQ_DEFAULT_PASS: secret
  #   healthcheck:
  #     test: ["CMD", "rabbitmq-diagnostics", "ping"]
  #     interval: 10s
  #     timeout: 5s
  #     retries: 5

  # ── Kafka + Zookeeper (uncomment if required by ADR) ──────────────────────
  # zookeeper:
  #   image: confluentinc/cp-zookeeper:7.6.0
  #   environment:
  #     ZOOKEEPER_CLIENT_PORT: 2181
  #
  # kafka:
  #   image: confluentinc/cp-kafka:7.6.0
  #   ports:
  #     - "9092:9092"
  #   environment:
  #     KAFKA_BROKER_ID: 1
  #     KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
  #     KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
  #     KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
  #   depends_on:
  #     - zookeeper
  #   healthcheck:
  #     test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
  #     interval: 10s
  #     timeout: 5s
  #     retries: 10

volumes:
  postgres_data:
  # mongo_data:
```

### Docker Rules

1. **Never use `COPY target/*.jar`** without a preceding build stage — the `target/` directory must not exist on the host.
2. **Dependency caching:** always `COPY pom.xml` / `package.json` before copying source so Docker layer cache avoids re-downloading dependencies on every build.
3. **Run as non-root** in the runtime stage.
4. **Use `healthcheck`** on all stateful services so `depends_on: condition: service_healthy` works correctly.
5. **Environment variables only** — never hardcode credentials in Dockerfiles; use `docker-compose.yml` `environment:` or `.env` file.
6. **`.dockerignore`** must exist in both `backend/` and `frontend/` to exclude `target/`, `node_modules/`, `.git/`, etc.

```
# backend/.dockerignore
target/
.mvn/
*.md
.git/

# frontend/.dockerignore
node_modules/
dist/
.git/
*.md
```

---

## ✅ Implementation Checklist (per Story)

- [ ] Domain entity / document created
- [ ] Flyway migration written (SQL) or document schema set (NoSQL)
- [ ] Repository interface defined
- [ ] Service class implemented with `@Transactional`
- [ ] DTOs (Request + Response) created
- [ ] MapStruct mapper created
- [ ] Controller created with all endpoints
- [ ] OpenAPI annotations on all endpoints
- [ ] Messaging producer/consumer added (if required by ADR)
- [ ] External API client added (if required by ADR)
- [ ] Global exception handler covers new exception types
- [ ] Story ID referenced in all new classes/methods (`// Story: US-[NNN]`)
- [ ] `application.yml` updated with new config keys
- [ ] Unit tests for service layer
- [ ] `Dockerfile` uses multi-stage build (no dependency on host `target/`)
- [ ] `.dockerignore` excludes `target/`, build artifacts, and dev files
- [ ] `docker-compose.yml` uses `build: context:` — not pre-built images for app services

---

## 💬 Interaction Guidelines

- **Always implement exactly what the ADR specifies.** Do not invent persistence or communication strategies.
- **Reference the Story ID** in every class header comment.
- **Never expose JPA entities** through REST responses.
- **Always version APIs:** `/api/v1/`.
- **Raise conflicts** if the ADR is ambiguous or contradicts existing code before implementing.
