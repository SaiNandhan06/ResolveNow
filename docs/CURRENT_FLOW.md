# ResolveNow - Current flow

This document describes the implementation currently in the repository.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka Server | 9000 | Service registry |
| API Gateway | 9080 | JWT validation, identity headers, routing |
| Auth Service | 8081 | Customer registration, login, seeded operational users |
| Complaint Service | 8082 | Complaint lifecycle and SLA escalation |
| Assignment Service | 8083 | Department routing, least-loaded agent selection |
| Notification Service | 8084 | User notifications and read state |

Each persistence service uses its own PostgreSQL database:
`authdb`, `complaintdb`, `assignmentdb`, and `notificationdb`.

## Create and assignment flow

1. A customer registers or logs in through the Gateway.
2. Auth Service issues a JWT containing the user ID, role, and department.
3. Gateway validates the JWT and injects `X-User-Id` and `X-User-Role`.
4. Complaint Service creates an `OPEN` complaint and calculates priority/SLA.
5. Complaint Service calls Assignment Service using Feign and an internal token.
6. Assignment Service maps category to department and selects the least-loaded enabled agent.
7. Assignment Service updates the complaint to `ASSIGNED`.
8. Notification Service stores messages for the customer and assigned agent.

## Lifecycle

```text
OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
                         |
                         +-> ESCALATED -> ASSIGNED
```

- Assigned agents use `PUT /api/assignments/{assignmentId}/accept`.
- Assigned agents use `PUT /api/assignments/{assignmentId}/resolve`.
- Customers use `POST /api/complaints/{id}/close` after resolution.
- Customers can escalate with `PUT /api/complaints/{id}/escalate`.
- The SLA scheduler escalates overdue active complaints automatically.

## Security boundaries

- Public endpoints are registration and login only.
- Customers can create and access only their own complaints and notifications.
- Agents can access their assignment queue and act only on their own assignments.
- Assignment, notification creation, status callbacks, escalation callbacks, and agent lookup require `X-Internal-Token`.
- Gateway removes client-supplied identity/internal headers before forwarding verified values.
- Public registration always creates a `CUSTOMER`; operational users are seeded by Auth Service.

## Resilience and validation

- Feign fallbacks log downstream failures.
- Service APIs return structured errors for validation, authorization, missing records, downstream failures, and unexpected errors.
- Complaint status transitions are validated rather than accepting arbitrary strings.
- Unit tests cover complaint lifecycle rules, assignment balancing, notification ownership, and customer-only registration.
- H2 test databases isolate context tests from local PostgreSQL.

## Deployment

`docker-compose.yml` runs PostgreSQL, Eureka, all services, and two Complaint Service instances. Set
`JWT_SECRET` and `INTERNAL_SERVICE_TOKEN`, then run:

```text
docker compose up --build
```
