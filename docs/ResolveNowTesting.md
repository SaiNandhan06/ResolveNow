# ResolveNow testing guide

## Automated validation

From the repository root:

```text
mvn test
mvn -DskipTests package
```

The suite includes service context tests, H2-isolated persistence tests, lifecycle and ownership unit tests,
least-loaded assignment tests, customer-only registration tests, and controller authorization tests.

## Manual lifecycle flow

Use the Gateway at `http://localhost:9080`.

1. Register a customer with `POST /api/auth/register`.
2. Log in with `POST /api/auth/login`.
3. Create a complaint with `POST /api/complaints`.
4. Read it with `GET /api/complaints/{id}`.
5. Log in as a seeded agent and read `GET /api/assignments/my`.
6. Accept with `PUT /api/assignments/{assignmentId}/accept`.
7. Resolve with `PUT /api/assignments/{assignmentId}/resolve`.
8. Log in as the customer and close it with `POST /api/complaints/{id}/close`.
9. Read notifications with `GET /api/notifications/my`.
10. Mark a notification read with `PUT /api/notifications/{id}/read`.

Seeded operational users use `SEED_USER_PASSWORD`, defaulting to `password`.

## Deployment validation

Set `JWT_SECRET` and `INTERNAL_SERVICE_TOKEN`, then run:

```text
docker compose config
docker compose build
docker compose up
```

Compose starts PostgreSQL, Eureka, all services, and two Complaint Service instances with the same application
name for load-balancing verification.
