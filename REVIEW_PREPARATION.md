# ResolveNow review preparation

This guide explains the implemented project in review-friendly language. The system is a Spring Boot
microservices complaint-resolution platform. The expected review focus is separation of services, secure
communication, complete lifecycle handling, testing, and deployability.

## 1. One-minute explanation

ResolveNow receives a customer complaint through the API Gateway. The Gateway verifies the customer's JWT
and forwards trusted identity headers. Complaint Service stores the complaint in `complaintdb`, then calls
Assignment Service through Eureka-discovered OpenFeign. Assignment Service maps the category to a department,
selects the least-loaded enabled agent, updates the complaint status, and asks Notification Service to persist
messages. Each service owns its own database and services communicate by IDs and Feign, never by cross-service
foreign keys.

The lifecycle is:

```text
OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
                         |
                         +-> ESCALATED -> ASSIGNED
```

## 2. Important locations

| Concern | Location |
|---|---|
| Gateway JWT validation | `apiGateway/src/main/java/resolvenow/filter/JwtAuthFilter.java` |
| Trusted header wrapper | `apiGateway/src/main/java/resolvenow/filter/HeaderMapRequestWrapper.java` |
| Auth endpoints | `authService/src/main/java/resolvenow/controller/AuthController.java` |
| JWT creation | `authService/src/main/java/resolvenow/security/JwtUtil.java` |
| Staff seeding | `authService/src/main/java/resolvenow/config/DefaultUserSeeder.java` |
| Complaint endpoints | `complaintService/src/main/java/resolvenow/controller/ComplaintController.java` |
| Complaint lifecycle/SLA | `complaintService/src/main/java/resolvenow/service/impl/ComplaintServiceImpl.java` |
| Assignment and balancing | `assignmentService/src/main/java/resolvenow/service/impl/AssignmentServiceImpl.java` |
| Notification ownership | `notificationService/src/main/java/resolvenow/controller/NotificationController.java` |
| Service discovery | Each service `src/main/resources/application.properties`; Eureka is `eurekaServer` |
| Docker topology | `docker-compose.yml` |
| Database creation | `docker/init-databases.sql` |
| Automated tests | Each service `src/test/java/resolvenow/` |

## 3. Roles and permissions

| Role | Main permissions |
|---|---|
| `CUSTOMER` | Register/login, create complaints, view own complaints, escalate, close after resolution, view own notifications |
| `AGENT` | View own active/history queue, accept own assignment, resolve own assignment |
| `SUPERVISOR` | Read operational complaint/assignment data according to service rules |
| `ADMIN` | Operational/read access according to service rules |

Public registration always creates `CUSTOMER`; clients cannot self-register as `AGENT`, `SUPERVISOR`, or `ADMIN`.
Operational users are seeded by Auth Service. The local default seed password is `password`, and it can be
overridden with `SEED_USER_PASSWORD`.

## 4. JWT and security flow

1. Auth Service hashes passwords with BCrypt.
2. Login/registration returns an access JWT and refresh-token value.
3. The JWT contains `sub` (user ID), `role`, `department`, `iat`, and `exp`.
4. The client sends `Authorization: Bearer <accessToken>` to the Gateway.
5. Gateway validates the signature and expiry using `JWT_SECRET`.
6. Gateway removes client-supplied identity/internal headers.
7. Gateway adds trusted `X-User-Id` and `X-User-Role` headers from verified claims.
8. Internal Feign callbacks require `X-Internal-Token`.

Secrets are environment variables in Docker. Never commit real JWT or internal tokens.

## 5. Service-to-service communication

The services use Eureka names rather than hard-coded container addresses:

```text
Complaint -> Assignment: create assignment
Assignment -> Complaint: update status
Assignment -> Auth: find enabled agents
Assignment -> Notification: create notification
Complaint -> Notification: lifecycle notification
```

OpenFeign clients use the shared internal-token configuration. Resilience4j circuit-breaker settings and
fallback logging protect the synchronous MVP from immediate cascading failures. The architecture intentionally
remains synchronous; Kafka/RabbitMQ is a future evolution, not required for the current rubric.

## 6. Docker and deployment

Required environment variables:

```text
JWT_SECRET=<long shared secret>
INTERNAL_SERVICE_TOKEN=<internal service secret>
```

Start and inspect the deployment:

```text
docker compose config
docker compose build
docker compose up -d
docker compose ps
curl http://localhost:9000/eureka/apps
curl http://localhost:9080/actuator/health
```

Compose starts PostgreSQL 18, Eureka, Auth, Complaint, Assignment, Notification, Gateway, and a second
Complaint Service instance. Both complaint instances register as `COMPLAINTSERVICE`, which demonstrates the
load-balancing topology. PostgreSQL uses separate databases: `authdb`, `complaintdb`, `assignmentdb`, and
`notificationdb`.

## 7. Manual REST-client walkthrough

Base URL: `http://localhost:9080`

### Register customer

`POST /api/auth/register`

```json
{
  "username": "reviewcustomer",
  "email": "reviewcustomer@example.com",
  "password": "Passw0rd!"
}
```

Expected: `200 OK`, save `accessToken` and `userId`.

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<uuid>",
  "role": "CUSTOMER",
  "userId": 1,
  "department": null
}
```

### Create complaint

`POST /api/complaints`

Headers: `Authorization: Bearer <customerToken>`

```json
{
  "title": "Invoice mismatch",
  "description": "The invoice amount is incorrect",
  "category": "BILLING"
}
```

Expected: `200 OK` with `status: "OPEN"` in the immediate response. A later GET normally shows `ASSIGNED`
after the Feign assignment call completes.

### Read complaint

`GET /api/complaints/{complaintId}`

Header: customer Bearer token. Expected status progression includes `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`,
and `CLOSED`.

### Agent login

`POST /api/auth/login`

```json
{
  "username": "agent-billing-1",
  "password": "password"
}
```

Expected role: `AGENT`, department: `BILLING`.

### Agent queue and actions

`GET /api/assignments/my` with the agent token.

Then:

```text
PUT /api/assignments/{assignmentId}/accept
PUT /api/assignments/{assignmentId}/resolve
```

Expected transitions: `ASSIGNED -> IN_PROGRESS -> RESOLVED`.

### Customer close

`POST /api/complaints/{complaintId}/close` with the customer token.

Expected: complaint status `CLOSED`.

### Notifications

`GET /api/notifications/my` with the customer token.

Expected messages include assignment, work-started, resolved, or closed lifecycle notifications.

`PUT /api/notifications/{notificationId}/read` marks one notification read.

## 8. Error and security checks to demonstrate

| Check | Expected result |
|---|---|
| Protected endpoint without Bearer token | `401 Unauthorized` |
| Invalid/expired JWT | `401 Unauthorized` |
| Customer tries agent accept endpoint | `403 Forbidden` |
| Customer requests another user's notification | `403 Forbidden` |
| Client sends spoofed `X-User-Id` | Gateway ignores it and uses JWT identity |
| Internal callback without `X-Internal-Token` | `403 Forbidden` |
| Blank complaint title/description/category | `400 Bad Request` with structured JSON |
| Missing complaint/assignment | structured `4xx` error |

## 9. Testing evidence

Run:

```text
mvn test
mvn -DskipTests package
git diff --check
```

The suite covers service context startup on H2, complaint lifecycle and ownership rules, least-loaded
assignment, customer-only registration, notification ownership, controller authorization, and repository
persistence. GitHub Actions repeats test, package, Compose validation, and Docker image builds on pushes and
pull requests.

## 10. Honest limitations and next-step answers

- Feign communication is synchronous; an event bus would improve decoupling at higher scale.
- Fallbacks log downstream failures; durable retry/outbox reconciliation is not yet implemented.
- Refresh tokens are issued and persisted, but a refresh endpoint is not part of the current API.
- Distributed tracing and centralized log aggregation are future operational enhancements.
- CI builds images but does not push or deploy them because registry credentials and production infrastructure
  are intentionally absent.
