# ResolveNow — Enterprise Issue Escalation & Service Desk Management System

An automated ticket-resolution microservices platform: a user raises a complaint, the system automatically routes it to the right department, and the user is notified at every stage of resolution — with full transparency into who has it and what's happening.

## Quick start

Clone the repository and enter the project directory:

```bash
git clone https://github.com/SaiNandhan06/ResolveNow.git
cd ResolveNow
```

Create the four PostgreSQL databases listed in [Prerequisites](#prerequisites), configure `DB_PASSWORD` and `JWT_SECRET`, then build the complete Maven reactor:

```bash
mvn clean package -DskipTests
```

Start the services in the order described in [Running locally](#running-locally). Use the API Gateway at `http://localhost:9080` for application requests.

---

## Problem statement

Manual service-desk workflows fail at scale: complaints get logged inconsistently, assignment to the right department depends on someone remembering the right routing rules, and users are left checking back repeatedly for updates with no visibility into where their ticket actually stands.

ResolveNow was built to solve this for a service-desk operator that needed to:
- Log every customer complaint through a consistent, auditable intake process
- Assign each complaint to the correct specialized department automatically, not by manual triage
- Keep the customer informed automatically at every lifecycle change, without them having to ask
- Stay responsive and available during high-volume support surges, not degrade or drop requests
- Secure every request end-to-end, with no service trusting unauthenticated traffic
- Be built as independently deployable services, not a single monolith, so any one part (intake, assignment, notification) can be scaled, replaced, or extended without touching the others

## How this solves it — key features

| Problem | ResolveNow's solution |
|---|---|
| Inconsistent complaint intake | A single, validated REST endpoint (`Complaint Service`) is the only way a complaint enters the system, with a defined lifecycle (`OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED`) |
| Manual/inconsistent routing | `Assignment Service` automatically maps complaint category → department the instant a complaint is created, with an auditable assignment record |
| Users left in the dark | `Notification Service` is triggered on every status change, so the user always has a current record of what happened and when |
| No single secure entry point | `API Gateway` is the only externally reachable component; every request is JWT-validated before it reaches any business service |
| Services trusting each other blindly | Identity is validated once at the Gateway and forwarded via signed internal headers — no service re-implements its own auth logic |
| Risk of one slow/broken service taking down the whole system | Independently deployable services registered via Eureka, ready for horizontal scaling and load balancing under a traffic surge |
| No visibility into system health/topology | Eureka's dashboard gives a live, real-time view of every registered service instance |

## Architecture

```
                         ┌─────────────────────┐
                         │   Eureka Server     │  service registry, :9000
                         └──────────┬──────────┘
                       register/heartbeat
        ┌───────────────────────────┼──────────────────────────────┐
        │                           │                              │
┌───────▼───────┐   ┌──────────────▼─────────┐   ┌─────────────────▼──────────┐
│  authService  │   │   complaintService     │   │   assignmentService        │
│  :8081        │   │   :8082                │   │   :8083                    │
└───────┬───────┘   └──────────────┬─────────┘   └─────────────────┬──────────┘
        │                          │  Feign: triggerAssignment()   │
        │                          └───────────────┬───────────────┘
        │                                          │ Feign: updateStatus() / create()
        │                              ┌─────────────▼──────────────┐
        │                              │   notificationService      │
        │                              │   :8084                    │
        │                              └────────────────────────────┘
        │
┌───────▼───────────────────────────────────────────────────────────────────┐
│              apiGateway (Spring Cloud Gateway Server WebMVC) :9080        │
│   - JwtAuthFilter validates the token, forwards X-User-Id/X-User-Role     │
│   - Routes /api/auth/**, /api/complaints/**, /api/assignments/**,         │
│     /api/notifications/** to the matching service via lb://<name>         │
└───────────────────────────────────────────────────────────────────────────┘
```

Full component breakdown, database schema, and security model: see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## Workflow — a complaint's journey

1. User registers/logs in → `authService` issues a signed JWT.
2. User submits a complaint through the Gateway → `complaintService` saves it with `status = OPEN`.
3. `complaintService` calls `assignmentService` (Feign) to trigger routing.
4. `assignmentService` maps the complaint's category to a department, records the assignment, then calls back into `complaintService` (`status = ASSIGNED`) and into `notificationService` to alert the user.
5. An agent updates the complaint's status as work progresses (`IN_PROGRESS → RESOLVED → CLOSED`) — each transition can trigger another notification.
6. The user can query their complaint, its assignment record, and their full notification history at any time — full transparency into the resolution process, end to end.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build | Maven (multi-module reactor, WAR packaging) |
| Service discovery | Netflix Eureka (Spring Cloud) |
| Gateway | Spring Cloud Gateway Server WebMVC |
| Inter-service calls | OpenFeign |
| Security | Spring Security + JWT (`jjwt`) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 18 (database-per-service) |
| Config | `application.properties` |

## Services

| Service | Port | Responsibility |
|---|---|---|
| `eurekaServer` | 9000 | Service registry |
| `apiGateway` | 9080 | Single entry point, JWT validation, routing |
| `authService` | 8081 | Registration, login, JWT issuing |
| `complaintService` | 8082 | Complaint lifecycle |
| `assignmentService` | 8083 | Auto-assignment to departments |
| `notificationService` | 8084 | User notifications on status changes |

## Prerequisites
- JDK 21
- Maven
- PostgreSQL 18, with four databases created: `authdb`, `complaintdb`, `assignmentdb`, `notificationdb`
- Eclipse IDE with the Spring Tools 4 plugin (or any IDE with Maven support)

## Environment variables

| Variable | Used by | Purpose |
|---|---|---|
| `JWT_SECRET` | `authService`, `apiGateway` | Shared signing key for JWTs — must be identical in both |
| `DB_PASSWORD` | all business services | PostgreSQL password (defaults to `postgres` if unset) |

Never commit real values for these.

## Running locally

Start in this exact order (each depends on the previous being registered in Eureka):
```
1. eurekaServer        → http://localhost:9000
2. authService
3. complaintService
4. assignmentService
5. notificationService
6. apiGateway           → http://localhost:9080  (all external traffic goes through here)
```
Run each with `Run As → Spring Boot App` in Eclipse, or `mvn spring-boot:run` from each module.

## API reference (via the Gateway, `http://localhost:9080`)

| Method | Endpoint | Auth required | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Log in, receive JWT |
| POST | `/api/complaints` | Yes | Create a complaint |
| GET | `/api/complaints/{id}` | Yes | Get a complaint |
| PUT | `/api/complaints/{id}/status` | Internal | Update complaint status (called by Assignment Service) |
| POST | `/api/assignments` | Internal | Trigger auto-assignment (called by Complaint Service) |
| GET | `/api/assignments/{complaintId}` | Yes | Get assignment for a complaint |
| POST | `/api/notifications` | Internal | Create a notification (called by Assignment Service) |
| GET | `/api/notifications/user/{userId}` | Yes | List a user's notifications |
| PUT | `/api/notifications/{id}/read` | Yes | Mark a notification read |

## Project structure (per service)
```
resolvenow
├── <Service>Application.java
├── model/            → JPA entities
├── repo/             → Spring Data JPA repositories
├── service/          → business logic (interface + impl/)
├── controller/       → REST controllers
├── dto/              → request/response shapes (where needed)
├── client/           → Feign clients calling other services (where needed)
├── security/         → JWT issuing/validation (Auth Service only)
└── config/           → Spring Security config (Auth Service only)
```

## Roadmap
Completed: Eureka registry, Auth (JWT), Gateway with header forwarding, Complaint/Assignment/Notification services, manual end-to-end verification.
Planned: automated tests, multi-instance load balancing, rate limiting, Kafka/RabbitMQ event backbone, centralized logging, AI-assisted triage, real-time transparency dashboard, Dockerization/CI-CD.
Full detail: see [`FEATURES_ROADMAP.md`](./FEATURES_ROADMAP.md).

## Documentation
- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — full system design, database schema, security model
- [`FEATURES_ROADMAP.md`](./FEATURES_ROADMAP.md) — planned features and why they matter
- [`PROJECT_CONTEXT.md`](./PROJECT_CONTEXT.md) — full project state for a developer or AI agent picking up this codebase

## License
Add your preferred license here (e.g. MIT) before making the repository public.
