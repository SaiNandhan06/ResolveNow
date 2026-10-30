# ResolveNow Manual Setup and Testing Guide

This guide covers cloning ResolveNow, importing it into Eclipse, configuring Docker and environment
variables, running the system before production, and manually testing every API route.

## 1. Prerequisites

Install the following before starting:

- Git
- JDK 21
- Eclipse IDE for Enterprise Java and Web Developers, or Eclipse with Spring Tools 4
- Maven 3.9+ (the repository also contains Maven Wrapper files)
- Docker Desktop with the Docker Engine running
- A REST client such as VS Code REST Client, Postman, or Insomnia

Check the tools:

```powershell
git --version
java -version
mvn -version
docker --version
docker compose version
```

Java must report version 21. Docker commands must work without a daemon connection error.

## 2. Clone the repository

Choose a development folder and run:

```powershell
git clone https://github.com/SaiNandhan06/ResolveNow.git
cd ResolveNow
git status
```

The expected branch is `main`. Do not commit `.env`; it is intentionally excluded by `.gitignore`.

## 3. Import into Eclipse

1. Open Eclipse.
2. Select **File > Import**.
3. Choose **Maven > Existing Maven Projects**.
4. Select the cloned `ResolveNow` folder.
5. Confirm the root `pom.xml` is detected.
6. Select all discovered modules and click **Finish**.
7. Wait for Maven and Eclipse to finish downloading dependencies and indexing.
8. Select **Window > Show View > Problems** and confirm there are no build-path errors.
9. Select **Project > Clean**, choose the ResolveNow projects, and rebuild if Eclipse shows stale errors.

The six applications are:

| Application | Responsibility | Port |
|---|---|---:|
| `eurekaServer` | Service discovery | 9000 |
| `apiGateway` | Public entry point, JWT validation, routing | 9080 |
| `authService` | Registration, login, JWT creation | 8081 |
| `complaintService` | Complaint lifecycle and SLA handling | 8082 |
| `assignmentService` | Department routing and agent assignment | 8083 |
| `notificationService` | Notification storage and read state | 8084 |

For normal development, use Docker Compose because it starts the complete dependency graph and both
Complaint Service instances. If running from Eclipse instead, start applications in this order:

1. Eureka Server
2. Auth Service
3. Complaint Service
4. Assignment Service
5. Notification Service
6. API Gateway

Use the `@SpringBootApplication` main class in each module. Requests should still be sent through
`http://localhost:9080`, not directly to business-service ports.

## 4. Configure the `.env` file

Create a file named `.env` in the repository root, next to `docker-compose.yml`:

```dotenv
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
INTERNAL_SERVICE_TOKEN=replace-with-a-different-random-internal-token
```

For the requested local Docker setup, the PostgreSQL password is already configured as `password` in
Compose. The database user is `postgres`; no extra database credentials need to be collected.

### Where values come from

| Variable | Purpose | Local value/source |
|---|---|---|
| `JWT_SECRET` | Signs and validates access tokens | Generate a private random secret; use the same value in every service |
| `INTERNAL_SERVICE_TOKEN` | Protects Feign callback endpoints | Generate a separate private random token; use the same value in internal-calling services |
| `DB_PASSWORD` | PostgreSQL password | Compose already sets it to `password` |
| `SEED_USER_PASSWORD` | Optional password for seeded staff | Defaults to `password`; set it explicitly for a safer environment |

Generate local secrets with PowerShell:

```powershell
$bytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
[Convert]::ToBase64String($bytes)
```

Run the command twice and use different results for `JWT_SECRET` and `INTERNAL_SERVICE_TOKEN`.
Never use a real production secret in screenshots, Git commits, tickets, or chat messages. In production,
collect secrets from the organization’s secret manager or CI/CD secret store, not from this repository.

## 5. Start and verify Docker

From the repository root:

```powershell
docker compose config
docker compose build
docker compose up -d
docker compose ps
```

`docker compose config` must complete without a missing-variable error. `docker compose ps` should show
PostgreSQL, Eureka, Gateway, Auth, Complaint (two instances), Assignment, and Notification containers
running.

Useful checks:

```powershell
Invoke-WebRequest http://localhost:9000/eureka/apps
Invoke-WebRequest http://localhost:9080/actuator/health
docker compose logs --tail=100 eurekaServer
docker compose logs --tail=100 apiGateway
```

The Eureka response should contain registered services. The Gateway health response should be `UP`.
Allow a short startup period before testing business endpoints because services register with Eureka
after their containers start.

Stop the local deployment:

```powershell
docker compose down
```

Use `docker compose down -v` only when intentionally deleting local PostgreSQL data and starting with
empty databases.

## 6. REST client variables

Set these variables in Postman, Insomnia, or a REST Client environment:

```text
baseUrl = http://localhost:9080
customerToken = <customer accessToken>
agentToken = <agent accessToken>
complaintId = <created complaint ID>
assignmentId = <assignment ID returned by the assignment endpoint>
notificationId = <notification ID returned by notifications>
```

For every protected public request, use:

```text
Authorization: Bearer <accessToken>
Content-Type: application/json
```

The Gateway extracts the user identity from the JWT. Do not manually add `X-User-Id` or
`X-User-Role`; those are trusted headers injected by the Gateway.

## 7. Manual endpoint test sequence

### 7.1 Register a customer

```http
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
  "username": "manualcustomer",
  "email": "manualcustomer@example.com",
  "password": "Passw0rd!"
}
```

Expected: `200 OK`. Save `accessToken` as `customerToken` and save `userId`.
Public registration must create a `CUSTOMER`, even if a caller tries to submit an operational role.

### 7.2 Log in

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "manualcustomer",
  "password": "Passw0rd!"
}
```

Expected response fields:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<token>",
  "role": "CUSTOMER",
  "userId": 1,
  "department": null
}
```

The exact numeric `userId` can differ between database runs.

### 7.3 Create and read a complaint

```http
POST {{baseUrl}}/api/complaints
Authorization: Bearer {{customerToken}}
Content-Type: application/json

{
  "title": "Manual billing issue",
  "description": "The invoice amount is incorrect",
  "category": "BILLING"
}
```

Expected: `200 OK`. Save the returned complaint `id` as `complaintId`. The immediate response can show
`OPEN`; the later GET should normally show `ASSIGNED` after automatic Feign assignment completes.

```http
GET {{baseUrl}}/api/complaints/{{complaintId}}
Authorization: Bearer {{customerToken}}
```

```http
GET {{baseUrl}}/api/complaints/my
Authorization: Bearer {{customerToken}}
```

Expected: only this customer's complaints are returned.

### 7.4 Log in as a seeded agent

The Auth Service seeds two agents per department. The default local password is `password`.
For the BILLING complaint, use:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "agent-billing-1",
  "password": "password"
}
```

Expected role: `AGENT`; expected department: `BILLING`. Save the returned access token as `agentToken`.
Other seeded departments are `technical`, `hr`, `logistics`, and `general`.

### 7.5 Check assignment and complete the lifecycle

```http
GET {{baseUrl}}/api/assignments/my
Authorization: Bearer {{agentToken}}
```

Find the assignment for `complaintId` and save its `id` as `assignmentId`.

```http
PUT {{baseUrl}}/api/assignments/{{assignmentId}}/accept
Authorization: Bearer {{agentToken}}
```

Expected: `200 OK`; complaint becomes `IN_PROGRESS`.

```http
PUT {{baseUrl}}/api/assignments/{{assignmentId}}/resolve
Authorization: Bearer {{agentToken}}
```

Expected: `200 OK`; complaint becomes `RESOLVED`.

Verify the state:

```http
GET {{baseUrl}}/api/complaints/{{complaintId}}
Authorization: Bearer {{customerToken}}
```

Expected lifecycle:

```text
OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED
```

### 7.6 Customer close and notifications

```http
POST {{baseUrl}}/api/complaints/{{complaintId}}/close
Authorization: Bearer {{customerToken}}
```

Expected: `200 OK`; complaint becomes `CLOSED`.

```http
GET {{baseUrl}}/api/notifications/my
Authorization: Bearer {{customerToken}}
```

Expected: notifications for assignment, work started, resolution, or closure. Save one notification
`id` as `notificationId`.

```http
PUT {{baseUrl}}/api/notifications/{{notificationId}}/read
Authorization: Bearer {{customerToken}}
```

```http
PUT {{baseUrl}}/api/notifications/my/read-all
Authorization: Bearer {{customerToken}}
```

Expected: `200 OK` for both read operations.

## 8. Remaining public routes

Run these after creating another complaint if the first complaint has already been closed:

| Method | Route | Token/role | Expected purpose |
|---|---|---|---|
| `PUT` | `/api/complaints/{id}/escalate` | Customer | Manually escalates an owned complaint |
| `GET` | `/api/assignments/{complaintId}` | Any authenticated role | Reads the assignment; customer is restricted to their own complaint |
| `GET` | `/api/assignments/my/history` | Agent | Reads resolved/escalated assignment history |
| `GET` | `/api/notifications/user/{userId}` | Same user only | Backward-compatible notification lookup |

For escalation:

```http
PUT {{baseUrl}}/api/complaints/{{complaintId}}/escalate
Authorization: Bearer {{customerToken}}
```

Expected: `200 OK` if the complaint is in a state that can be escalated, followed by reassignment.

## 9. Internal endpoint inventory

These routes are used only for service-to-service communication. They require the exact
`X-Internal-Token` configured in the environment and should not be called through the public Gateway
as a customer or agent. They are listed for architecture review and controlled diagnostics.

| Method | Route | Caller | Expected |
|---|---|---|---|
| `POST` | `/api/assignments` | Complaint Service | Creates an automatic assignment |
| `POST` | `/api/assignments/{complaintId}/escalate` | Complaint Service | Reassigns an escalated complaint |
| `PUT` | `/api/complaints/{id}/status?status=ASSIGNED` | Assignment Service | Updates complaint lifecycle status |
| `POST` | `/api/notifications` | Complaint or Assignment Service | Persists a notification |

Controlled request shape examples:

```http
POST http://localhost:8083/api/assignments
X-Internal-Token: <INTERNAL_SERVICE_TOKEN>
Content-Type: application/json

{
  "complaintId": 123,
  "category": "BILLING",
  "userId": 1
}
```

```http
PUT http://localhost:8082/api/complaints/123/status?status=ASSIGNED
X-Internal-Token: <INTERNAL_SERVICE_TOKEN>
```

Expected: `200 OK` with the correct token, `403 Forbidden` when the header is missing or incorrect.
The exact request DTO fields for escalation and notification are implementation details; normal manual
testing should trigger them through complaint creation and lifecycle actions instead.

## 10. Security and validation tests

Record the status code and JSON error body for each check:

| Test | Request change | Expected |
|---|---|---|
| Missing authentication | Remove `Authorization` from a protected route | `401 Unauthorized` |
| Bad token | Use `Bearer not-a-real-token` | `401 Unauthorized` |
| Wrong role | Use customer token on `/api/assignments/{id}/accept` | `403 Forbidden` |
| Customer data isolation | Request another customer's complaint | `403` or structured `4xx` |
| Notification isolation | Call `/api/notifications/user/{otherUserId}` | `403 Forbidden` |
| Spoofed identity | Add client `X-User-Id` with a different value | Gateway must use JWT identity |
| Invalid input | Blank title, description, or category | `400 Bad Request` |
| Missing resource | Use a nonexistent complaint or assignment ID | Structured `4xx` |
| Internal protection | Call internal assignment/notification routes without `X-Internal-Token` | `403 Forbidden` |

Do not expose the internal token through the Gateway or use internal routes as public API calls.

## 11. Direct service health checks

Actuator health is exposed for each service. When running all services locally outside Docker, check:

```text
http://localhost:9000/actuator/health
http://localhost:9080/actuator/health
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
http://localhost:8083/actuator/health
http://localhost:8084/actuator/health
```

In the Docker setup, only the Gateway and Eureka ports are published to the host by default. Use
`docker compose ps` and container logs for the other services, or temporarily publish their ports only
for local diagnostics.

## 12. Automated checks before production

Run from the repository root:

```powershell
mvn test
mvn -DskipTests package
docker compose config
docker compose build
git diff --check
```

Before deploying, also confirm:

- No real secrets or `.env` files are committed.
- `JWT_SECRET` and `INTERNAL_SERVICE_TOKEN` are long, random, and different.
- The PostgreSQL password is replaced with a secret-manager value for production.
- `SEED_USER_PASSWORD` is changed from `password`.
- Docker images use a pinned, reviewed base image policy.
- Health endpoints are reachable from the deployment environment.
- Logs contain no passwords, JWTs, or internal tokens.
- Database backups, migrations, TLS, firewall rules, and a rollback plan exist.
- The Gateway is the only public application entry point.
- Production does not expose PostgreSQL, Eureka, or internal service ports publicly.

## 13. Troubleshooting

| Symptom | Action |
|---|---|
| Missing `JWT_SECRET` or `INTERNAL_SERVICE_TOKEN` | Create the root `.env`, then rerun `docker compose config` |
| Containers restart | Run `docker compose logs --tail=200 <service>` |
| Gateway returns `503` after startup | Wait for Eureka registration and inspect `docker compose logs eurekaServer` |
| Database connection failure | Confirm Docker Engine is running and use `docker compose ps` |
| Duplicate seeded-user error after experiments | Keep the existing database, or intentionally reset with `docker compose down -v` |
| Eclipse shows stale errors | Wait for Maven import, then run **Project > Clean** |
| Port already in use | Stop the conflicting process or change the host port mapping |

## 14. Shutdown and cleanup

Keep local data:

```powershell
docker compose down
```

Delete containers and the local PostgreSQL volume:

```powershell
docker compose down -v
```

The second command is destructive to local test data. Never use it against a production deployment.
