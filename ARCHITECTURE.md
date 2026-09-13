# ResolveNow — Architecture

## 1. Overview
ResolveNow decouples complaint intake, department assignment, and user notification into three independent microservices, fronted by a single API Gateway and coordinated via Eureka service discovery. Authentication is centralized in a dedicated Auth Service issuing JWTs.

## 2. Component diagram
```
                         ┌─────────────────────┐
                         │   Eureka Server      │  registry, :9000
                         └──────────┬───────────┘
                       register/heartbeat
        ┌───────────────────────────┼──────────────────────────────┐
        │                           │                              │
┌───────▼───────┐   ┌──────────────▼─────────┐   ┌─────────────────▼──────────┐
│  authService   │   │   complaintService       │   │   assignmentService          │
│  :8081         │   │   :8082                  │   │   :8083                     │
└───────┬───────┘   └──────────────┬─────────┘   └─────────────────┬──────────┘
        │                          │  Feign: triggerAssignment()   │
        │                          └───────────────┬────────────────┘
        │                                            │ Feign: updateStatus() / create()
        │                              ┌─────────────▼──────────────┐
        │                              │   notificationService       │
        │                              │   :8084                     │
        │                              └─────────────────────────────┘
        │
┌───────▼───────────────────────────────────────────────────────────────────┐
│                     apiGateway (Spring Cloud Gateway Server WebMVC) :9080  │
│   - JwtAuthFilter validates the token, wraps the request to add            │
│     X-User-Id / X-User-Role headers before forwarding                     │
│   - Routes /api/auth/**, /api/complaints/**, /api/assignments/**,         │
│     /api/notifications/** to the matching service via lb://<name>         │
└───────────────────────────────────────────────────────────────────────────┘
                                    ▲
                              ┌─────┴─────┐
                              │  Clients   │ (Postman / browser REST client)
                              └───────────┘
```

## 3. Request flow: creating a complaint
1. Client sends `POST /api/complaints` to the Gateway with a Bearer JWT.
2. `JwtAuthFilter` validates the signature/expiry, wraps the request, adds `X-User-Id`/`X-User-Role` headers.
3. Gateway routes to `complaintService` via Eureka + `lb://complaintService`.
4. `ComplaintController` reads `X-User-Id`, calls `ComplaintServiceImpl.createComplaint()`.
5. Complaint is saved with `status = OPEN`.
6. `AssignmentClient` (Feign) synchronously calls `assignmentService`'s `POST /api/assignments`.
7. `AssignmentServiceImpl.autoAssign()` maps category → department, saves an `Assignment` row, then:
   - calls back into `complaintService` via `ComplaintClient` (`PUT /api/complaints/{id}/status`) to set `status = ASSIGNED`
   - calls `notificationService` via `NotificationClient` (`POST /api/notifications`) to persist a notification
8. Client can then `GET /api/complaints/{id}`, `GET /api/assignments/{complaintId}`, and `GET /api/notifications/user/{userId}` to see the full result.

This is a synchronous, Feign-based chain for the MVP. A later phase replaces steps 6–7's direct calls with Kafka/RabbitMQ events for true decoupling.

## 4. Database design (database-per-service)

### `authdb`
```sql
CREATE TABLE users (
    user_id       BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) DEFAULT 'CUSTOMER',
    department_id BIGINT,
    enabled       BOOLEAN DEFAULT TRUE
);

CREATE TABLE refresh_tokens (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    token       VARCHAR(500) NOT NULL,
    expiry_date TIMESTAMP NOT NULL
);
```

### `complaintdb`
```sql
CREATE TABLE complaint (
    complaint_id BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    title        VARCHAR(150) NOT NULL,
    description  TEXT NOT NULL,
    category     VARCHAR(50),
    status       VARCHAR(20) DEFAULT 'OPEN',
    created_at   TIMESTAMP DEFAULT now()
);
```

### `assignmentdb`
```sql
CREATE TABLE assignment (
    assignment_id BIGSERIAL PRIMARY KEY,
    complaint_id  BIGINT NOT NULL,
    department    VARCHAR(50),
    assigned_to   BIGINT,
    status        VARCHAR(20) DEFAULT 'ASSIGNED',
    assigned_at   TIMESTAMP DEFAULT now()
);
```

### `notificationdb`
```sql
CREATE TABLE notification (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    complaint_id    BIGINT,
    message         TEXT NOT NULL,
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT now()
);
```
No cross-service foreign keys — services reference each other only by ID, resolved via Feign calls when needed.

## 5. Security model
- `authService` issues a signed JWT (`HS256`) on register/login containing `sub` (userId) and `role`.
- `apiGateway`'s `JwtAuthFilter` is the single point of token validation for all external traffic; it rejects unauthenticated requests before they reach any business service.
- On success, the filter wraps the request (`HeaderMapRequestWrapper`) to inject `X-User-Id`/`X-User-Role`, which downstream controllers trust because only the Gateway can set them on the internal network.
- Passwords are hashed with BCrypt (`SecurityConfig` in `authService`).
- `JWT_SECRET` must be identical between `authService` and `apiGateway`, supplied via environment variable — never committed.

## 6. Service discovery & routing
- Every business service registers with `eurekaServer` (`eureka.client.serviceUrl.defaultZone=http://localhost:9000/eureka`).
- `apiGateway` routes by path prefix to `lb://<serviceName>`, letting Eureka + Spring Cloud LoadBalancer resolve and balance across instances — this is the mechanism a later phase exercises with multiple `complaintService` instances.

## 7. Known simplifications (intentional, for MVP)
- Department is a hardcoded category → department string map in `AssignmentServiceImpl`, not a separate `Department` table — promote to an entity when dynamic department management is needed.
- Inter-service communication is synchronous (Feign), not event-driven (Kafka/RabbitMQ) — planned for a later phase.
- No centralized exception handling (`@ControllerAdvice`) yet — errors currently surface as generic 500s from unhandled `IllegalArgumentException`s.
- No rate limiting or circuit breakers yet — planned alongside multi-instance load balancing.
