# ResolveNow — Features Roadmap

Everything below is **not yet built**. This is the backlog, ordered by dependency (some items need earlier ones in place first), with why each matters and roughly where it plugs into the existing architecture.

---

## Immediate next — testing

### Automated test suite
- **What:** `@WebMvcTest` per controller, `@DataJpaTest` per repository, one `@SpringBootTest` happy-path per service, WireMock stubs for Feign calls between Complaint/Assignment/Notification.
- **Why now:** the codebase is still small enough to backfill tests cheaply; every feature below this line gets riskier to add without a safety net.
- **Where:** `src/test/java/resolvenow/...` in each service, mirroring the main package structure.

---

## Phase — scaling & resilience

### Multi-instance load balancing
- **What:** run a second instance of `complaintService` (e.g. port 8085) and confirm Eureka + the Gateway's `lb://` resolves and round-robins between both.
- **Why:** proves the "high-volume support surge" resilience requirement from the original spec — currently unverified since every service only ever runs as a single instance.
- **Where:** no new code — just run a second instance with a different `server.port`, same `spring.application.name`.

### Gateway rate limiting
- **What:** cap requests per user/IP at the Gateway (Spring Cloud Gateway's built-in rate limiter, or a Redis-backed token bucket).
- **Why:** protects against abuse and accidental retry storms before they reach any business service.
- **Where:** `apiGateway` — new filter or route-level config.

### Circuit breakers on Feign calls
- **What:** wrap `AssignmentClient`, `ComplaintClient`, `NotificationClient` with Resilience4j `@CircuitBreaker`/`@Retry`.
- **Why:** currently, if Assignment Service is down, Complaint Service's `createComplaint()` call blocks/throws with no graceful fallback — a slow or dead downstream service can cascade.
- **Where:** `complaintService`/`assignmentService`'s `client/` packages, plus a `resilience4j.circuitbreaker.*` block in each `application.properties`.

---

## Phase — event-driven architecture

### Kafka/RabbitMQ event backbone
- **What:** replace the direct Feign calls (Complaint → Assignment → Notification) with published/consumed events — e.g. `ComplaintCreated`, `ComplaintAssigned`, `ComplaintStatusChanged`.
- **Why:** true decoupling — right now Complaint Service's request blocks on Assignment Service being reachable; an event bus removes that runtime dependency entirely and matches the original "decoupled" requirement more faithfully than synchronous REST.
- **Where:** new `event/` package per service (publisher in Complaint/Assignment, listener in Assignment/Notification); the Feign `client/` classes get replaced, not kept alongside.
- **Dependency:** do this after circuit breakers above, so you understand the failure modes you're actually trying to remove.

### Centralized structured logging
- **What:** structured JSON logs from every service (and eventually the website backend too) shipped to a central store (ELK stack or Grafana Loki).
- **Why:** once requests span multiple services and an event bus, tracing a single complaint's journey through plain per-service console logs becomes impractical.
- **Where:** logging config in each `application.properties` (`logging.pattern.console` as JSON, or a dedicated appender), plus a log-shipping agent (Filebeat/Promtail) — infrastructure-level, not application code.

### Distributed tracing
- **What:** Micrometer Tracing + Zipkin/Jaeger to visualize a single complaint's full call chain across services.
- **Why:** with async events in place, "what happened to complaint #482" becomes a genuinely hard question without a trace ID threading through every hop.
- **Where:** add `micrometer-tracing-bridge-brave` + Zipkin exporter dependency to every service; mostly auto-configured once added.

---

## Phase — intelligence & transparency

### AI-assisted complaint categorization/routing
- **What:** instead of the user picking a category (or in addition to it), call an LLM to infer category/priority from the complaint description, and optionally draft a suggested first response.
- **Why:** reduces misrouted complaints and speeds up initial triage.
- **Where:** new call from `complaintService` (or a new dedicated `triageService`) to an LLM API at creation time, before the category is passed to `assignmentService`.
- **Dependency:** ideally sits on top of the event bus (Phase above), so triage can happen asynchronously without blocking complaint creation.

### Real-time transparency dashboard
- **What:** a live view (web dashboard) of complaint volume by status/department, SLA countdowns, and per-agent workload.
- **Why:** directly answers the "transparency tracking of resolution status" goal from the original problem statement — right now transparency is limited to a user querying their own complaint via API, not any kind of live operational view.
- **Where:** new frontend (React/Angular) + a read-optimized aggregation endpoint, likely fed by the event stream via a materialized view or a small dedicated `analyticsService`.

### SLA management & auto-escalation
- **What:** each complaint gets a due-by timestamp based on priority; a scheduled job checks for breaches and auto-escalates (reassigns to a supervisor, sends an urgent notification).
- **Why:** currently nothing prevents a complaint from sitting untouched indefinitely.
- **Where:** add `sla_due_at` to the `Complaint` entity, a `@Scheduled` job in `complaintService` or a dedicated `slaService`.

### Feedback / CSAT rating
- **What:** after a complaint reaches `CLOSED`, prompt the user for a 1–5 rating + comment.
- **Why:** closes the loop — currently there's no signal on whether a resolution actually satisfied the user.
- **Where:** new `Feedback` entity + endpoint, likely in `complaintService` or its own small service.

---

## Phase — platform & operations

### Dockerization
- **What:** a `Dockerfile` per service (multi-stage Maven build → slim JRE runtime) plus a root `docker-compose.yml` wiring all six services, Postgres, and (once added) Kafka.
- **Why:** required before any real deployment target; also makes onboarding a new developer trivial (`docker-compose up` instead of manually starting six services in order).
- **Where:** new `Dockerfile` in each service root, new `docker-compose.yml` at the repo root.

### CI/CD pipeline
- **What:** GitHub Actions workflow — build → test → (once Dockerized) build images → push → deploy.
- **Why:** catches breakage automatically instead of relying on manual `mvn clean install` runs.
- **Where:** `.github/workflows/build.yml` at the repo root.

### Config Server + secrets management
- **What:** Spring Cloud Config Server centralizing all `application.properties`, with real secrets (JWT_SECRET, DB passwords) pulled from HashiCorp Vault or a cloud secrets manager instead of environment variables.
- **Why:** environment variables work for local dev with six services, but don't scale cleanly to multiple environments (dev/staging/prod) or multiple instances.
- **Where:** new `configServer` module; every other service's `application.properties` gets replaced by a `bootstrap.properties` pointing at the Config Server.

### Multi-tenancy
- **What:** support multiple client organizations on one deployment via a `tenant_id` column and row-level isolation.
- **Why:** only relevant if ResolveNow is ever offered to more than one organization from a single deployment — lowest priority item here, include only if that becomes a real requirement.
- **Where:** touches every entity (`tenant_id` column) and every repository query.

---

## Summary — suggested build order
1. Automated tests
2. Multi-instance load balancing (cheapest proof of resilience)
3. Gateway rate limiting + circuit breakers
4. Kafka/RabbitMQ event backbone
5. Centralized logging + distributed tracing
6. AI-assisted triage
7. Transparency dashboard + SLA management + feedback
8. Dockerization + CI/CD
9. Config Server/secrets, multi-tenancy — only if/when actually needed
