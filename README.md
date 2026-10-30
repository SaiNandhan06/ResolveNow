# ResolveNow

ResolveNow is a Spring Boot microservices service-desk platform. Customers create complaints, the system routes them to the least-loaded enabled agent, tracks lifecycle status, escalates SLA breaches, and stores notifications.

## Architecture

```text
Client -> API Gateway -> Eureka-discovered services
                         ├─ Auth Service -> authdb
                         ├─ Complaint Service -> complaintdb
                         ├─ Assignment Service -> assignmentdb
                         └─ Notification Service -> notificationdb
```

Services communicate synchronously with OpenFeign. Each business service owns its database; there are no cross-service foreign keys.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka Server | 9000 | Service registry |
| API Gateway | 9080 | JWT validation and routing |
| Auth Service | 8081 | Registration, login, JWTs, seeded staff |
| Complaint Service | 8082 | Complaint lifecycle and SLA scheduler |
| Assignment Service | 8083 | Department routing and agent workload balancing |
| Notification Service | 8084 | Notification persistence and read state |

## Complaint lifecycle

```text
OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
                         |
                         +-> ESCALATED -> ASSIGNED
```

Customers create, view, escalate, and close their own complaints. Agents can view their queue, accept, and resolve only their assignments. Internal Feign callbacks require `X-Internal-Token`.

## Local setup

Requirements: JDK 21, Maven, PostgreSQL, and four databases:
`authdb`, `complaintdb`, `assignmentdb`, `notificationdb`.

Set:

```text
JWT_SECRET=<long shared signing secret>
DB_PASSWORD=password
INTERNAL_SERVICE_TOKEN=<internal service secret>
SEED_USER_PASSWORD=password
```

Then run:

```text
mvn test
mvn package
```

Start Eureka first, then Auth, Complaint, Assignment, Notification, and finally the Gateway. All external requests use `http://localhost:9080`.

## Docker deployment

The repository includes one multi-stage Dockerfile per service and a root `docker-compose.yml`. It starts PostgreSQL, Eureka, all services, and two Complaint Service instances:

```text
JWT_SECRET=<long shared signing secret>
INTERNAL_SERVICE_TOKEN=<internal service secret>
docker compose up --build
```

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - component and database design
- [CURRENT_FLOW.md](docs/CURRENT_FLOW.md) - implemented request flow and security boundaries
- [FEATURES_ROADMAP.md](docs/FEATURES_ROADMAP.md) - future enhancements
- [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) - conventions and continuation notes
- [ResolveNowTesting.md](docs/ResolveNowTesting.md) - API walkthrough

## Testing

The Maven suite includes service context tests isolated on H2 plus unit tests for complaint lifecycle rules, assignment balancing, notification ownership, and customer-only registration.
