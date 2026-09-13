# ResolveNow — Project Context (for a developer or AI agent picking up this codebase)

Read this file first, before touching any code. It's the single source of truth for conventions, current state, and past mistakes already fixed — the goal is that anyone (human or AI agent) can pick this project up cold and not repeat work or reintroduce bugs already solved.

---

## 1. What this project is

ResolveNow is an automated ticket/complaint resolution platform built as six independent Spring Boot microservices. See `README.md` for the full problem statement and feature list, `ARCHITECTURE.md` for the system design and database schema.

**Current status: MVP complete and manually verified end-to-end.** Automated tests, load balancing, event-driven messaging, and everything else in `FEATURES_ROADMAP.md` is **not yet built** — do not assume any of it exists.

---

## 2. Exact tech stack (do not silently change versions)

| Item | Value |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud BOM | `spring-cloud-dependencies` 2025.1.3 |
| Build tool | Maven, multi-module reactor |
| Packaging | `war` (every service) |
| Config format | `application.properties` — **never** `.yml` |
| Database | PostgreSQL 18, database-per-service |
| ORM | Spring Data JPA / Hibernate |
| Auth | Custom JWT via `jjwt` 0.12.x (not a full OAuth2 Authorization Server — see Section 6) |
| Gateway | `spring-cloud-starter-gateway-server-webmvc` — the **servlet-based** Gateway, not the reactive one (chosen specifically because every service is WAR-packaged) |
| Lombok | Required in `authService`, `complaintService`, `assignmentService`, `notificationService`. Not needed in `eurekaServer`/`apiGateway` (no entities/DTOs there) |

If asked to upgrade any of these, check actual current compatibility (e.g. Spring Cloud BOM ↔ Spring Boot version pairing) before changing — these were verified against real release notes at the time this was built, not guessed.

---

## 3. Naming conventions — apply these to any new code

| What | Convention | Example |
|---|---|---|
| Maven Group Id | `resolvenow` (no `com.` prefix) | |
| Maven Artifact Id | camelCase | `complaintService` |
| Java base package | plain lowercase, **identical across every service**: `resolvenow` | `resolvenow.model.Complaint` |
| Java classes/methods/fields | standard PascalCase/camelCase | `ComplaintController`, `getById()` |
| Database names | all lowercase (see Section 5 for why this matters) | `complaintdb`, not `complaintDb` |
| Custom `application.properties` keys | camelCase | `jwt.accessTokenExpiryMs` |
| Framework-owned `application.properties` keys | left as-is, don't rename | `spring.datasource.url` |

## 4. Folder structure convention — apply to any new service or new code in an existing one

Every service starts with exactly four packages. Add others **only when that specific service actually needs them** — don't add `dto`/`client`/`security`/`config` preemptively.

```
resolvenow
├── <Service>Application.java
├── model/            → JPA entities (always present if the service has persistence)
├── repo/             → Spring Data JPA repositories (always present if the service has persistence)
├── service/
│   ├── <X>Service.java         (interface)
│   └── impl/<X>ServiceImpl.java
├── controller/       → REST controllers (always present)
├── dto/              → add only if the controller shouldn't expose the entity directly
├── client/           → add only if this service calls another service (Feign)
├── security/         → add only if this service issues/parses JWTs (currently: authService only)
└── config/           → add only if this service needs Spring Security config (currently: authService only)
```
`eurekaServer` and `apiGateway` don't follow this — they have no persistence/domain logic. `apiGateway` instead has a `filter/` package for its JWT-validating servlet filter.

---

## 5. Known bugs already fixed — do not reintroduce these

**Read this section carefully before making changes to auth, the Gateway, or any `application.properties` file.**

### 5.1 Database name case-folding
PostgreSQL lowercases unquoted identifiers in `CREATE DATABASE`. Every database name in this project is **all-lowercase** (`authdb`, `complaintdb`, `assignmentdb`, `notificationdb`) specifically to avoid a mismatch between what's in `application.properties` and what Postgres actually created. **Never use camelCase or mixed-case database names anywhere in this project.**

### 5.2 Lombok not activating in Eclipse
Having `lombok` as a Maven dependency is not sufficient for Eclipse's own JDT compiler to recognize `@Getter`/`@Setter`/`@Builder`/`@RequiredArgsConstructor`-generated methods. Lombok's Eclipse agent must be installed separately (`java -jar lombok.jar` → point at the Eclipse install) and annotation processing enabled per-project. If you see "method X is undefined" errors on a class that clearly has `@Data`/`@Builder`, this is almost always the cause — it is not a real compile error in the source.

### 5.3 Gateway header forwarding requires a request wrapper
A plain servlet `Filter` (`JwtAuthFilter` in `apiGateway`) can read incoming headers but **cannot** inject new ones into the proxied outbound request without wrapping it. The fix already in place: `HeaderMapRequestWrapper` (in `apiGateway/src/main/java/resolvenow/filter/`) wraps the request and adds `X-User-Id`/`X-User-Role` before calling `chain.doFilter(wrappedRequest, res)`. If you touch `JwtAuthFilter`, preserve this wrapping — removing it silently breaks every downstream `@RequestHeader("X-User-Id")` parameter (e.g. in `ComplaintController`).

### 5.4 Cross-service DTOs cannot be shared by import
`assignmentService` and `complaintService` are separate Maven modules — a Java class in one cannot be imported into the other. Where both sides need the same JSON shape (e.g. `AssignmentTriggerRequest`), **each service defines its own identical local record/class**. They only need matching field names for Feign's JSON (de)serialization to work; they must never literally share a compiled class. If you add a new cross-service call, follow this same pattern — do not import a DTO from another service's `client/` package.

---

## 6. Deliberate simplifications — not bugs, don't "fix" without discussion

- **JWT is hand-rolled (`jjwt`), not a full Spring Authorization Server.** This was a deliberate MVP-speed tradeoff. Auth Service issues a token, the Gateway validates it with the same shared secret (`JWT_SECRET` env var). Upgrading to a real OAuth2 Authorization Server + JWKS-based resource servers is a valid future step, but is a meaningfully bigger change (see `FEATURES_ROADMAP.md`) — don't do it incidentally while fixing something else.
- **Department assignment is a hardcoded `Map<String,String>`** in `AssignmentServiceImpl` (category → department), not a `Department` database entity. Promote it to a real entity only when dynamic department management is actually needed.
- **Inter-service calls are synchronous Feign, not Kafka/RabbitMQ.** Complaint Service blocks on Assignment Service being reachable. This is intentional for the MVP; the event-driven rework is a planned, larger phase (see roadmap), not a quick fix.
- **No global exception handling yet.** Errors currently surface as generic 500s from unhandled `IllegalArgumentException`. Adding a `@ControllerAdvice`/`GlobalExceptionHandler` per service is reasonable to do anytime, low-risk.
- **The `createComplaint()` response shows `status: "OPEN"`, not `"ASSIGNED"`,** even though the DB row is updated to `ASSIGNED` almost immediately after, because the response is built from the in-memory object captured before the Feign call's side effects land. A subsequent `GET` shows the correct status. This is a known, accepted quirk, not a bug to silently patch unless asked.

---

## 7. How to run and verify the system

Startup order matters (each depends on the previous being Eureka-registered):
```
eurekaServer (9000) → authService (8081) → complaintService (8082) →
assignmentService (8083) → notificationService (8084) → apiGateway (9080)
```
All external traffic goes through `http://localhost:9080`. See `README.md`'s API reference table for every endpoint, and the earlier conversation history / `GIT_COMMIT_PLAN_GRANULAR.md` for a full sample request/response walkthrough (register → login → create complaint → check assignment → check notification).

Required environment variables: `JWT_SECRET` (shared identically between `authService` and `apiGateway`), `DB_PASSWORD` (defaults to `postgres` if unset).

---

## 8. If you are an AI agent continuing this project

- Read Sections 5 and 6 above **before** proposing any change to auth, the Gateway filter, database config, or cross-service DTOs — these represent real bugs already hit and fixed; don't re-derive or "improve" them without understanding why they're structured this way.
- Follow the naming and folder conventions in Sections 3–4 for any new service or file — consistency across all six services is a deliberate, stated goal of this project, not an accident.
- Check `FEATURES_ROADMAP.md` before building anything not explicitly requested — it lists what's planned, in what order, and why, so unsolicited feature work doesn't get built out of sequence or duplicate planned work.
- When you finish a unit of work, the project's convention (see `GIT_COMMIT_PLAN_GRANULAR.md`) is **one small commit per logical file/layer**, using Conventional Commits (`feat(scope): subject`, `fix(scope): subject`, `chore: subject`), with a body explaining *why* on any bug-fix commit — not one large commit per feature.
- No automated tests exist yet. If your change could plausibly break existing behavior, say so explicitly rather than assuming test coverage would have caught it.
