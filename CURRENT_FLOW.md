# Current flow (as implemented)

Mapped from live controllers, Feign clients, and JPA entities. This is what the code does today, not what the brief requires.

All external traffic: `http://localhost:9080` (API Gateway). JWT required except register/login.

## Services and ports

| Service | Port | Role today |
|---|---|---|
| eurekaServer | 9000 | Registry |
| apiGateway | 9080 | JWT filter, `lb://` routes |
| authService | 8081 | Register / login |
| complaintService | 8082 | Create + get + unconstrained status PUT |
| assignmentService | 8083 | Category → department string, `assignedTo = 1` |
| notificationService | 8084 | Persist in-app message; mark read |

## Endpoints

| Method | Path | Auth | Behavior |
|---|---|---|---|
| POST | `/api/auth/register` | No | Always `role = CUSTOMER`. Issues access JWT + unused refresh token |
| POST | `/api/auth/login` | No | Same tokens. No role change |
| POST | `/api/complaints` | JWT | Save `OPEN`, Feign `POST /api/assignments` |
| GET | `/api/complaints/{id}` | JWT | Return row. No ownership check |
| PUT | `/api/complaints/{id}/status` | JWT | Any status string. No role, no transitions, no notify |
| POST | `/api/assignments` | JWT | Map category → dept. Hardcode agent `1`. PUT status `ASSIGNED`. Notify customer once |
| GET | `/api/assignments/{complaintId}` | JWT | Return assignment |
| POST | `/api/notifications` | JWT | Create row |
| GET | `/api/notifications/user/{userId}` | JWT | List |
| PUT | `/api/notifications/{id}/read` | JWT | Mark read |

## Create-complaint chain (only complete path)

1. Customer `POST /api/auth/login` → JWT (`sub` = userId, `role`).
2. Gateway `JwtAuthFilter` adds `X-User-Id` / `X-User-Role`.
3. `POST /api/complaints` saves `status = OPEN`.
4. Feign `POST /api/assignments` with `complaintId`, `category`, `userId`.
5. Assignment maps `BILLING|TECHNICAL|HR|LOGISTICS` else `GENERAL`, sets `assignedTo = 1L`.
6. Feign `PUT /api/complaints/{id}/status?status=ASSIGNED`.
7. Feign `POST /api/notifications` — customer only: “assigned to {department}”.

README claims `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED`. Code only does `OPEN` then `ASSIGNED`. There is **no** start, escalate, resolve-with-notes, or close API.

## Tables today

**authdb.users:** `user_id`, `username`, `email`, `password_hash`, `role`, `department_id` (unused), `enabled`

**authdb.refresh_tokens:** written on every login, **never read** (no `/refresh` or `/logout`)

**complaintdb.complaint:** `complaint_id`, `user_id`, `title`, `description`, `category`, `status`, `created_at`

**assignmentdb.assignment:** `assignment_id`, `complaint_id`, `department`, `assigned_to`, `status` (duplicates complaint status), `assigned_at`

**notificationdb.notification:** `notification_id`, `user_id`, `complaint_id`, `message`, `is_read`, `created_at`

## Gaps vs the brief

- Lifecycle not tracked past `ASSIGNED`
- Assignment is not to a real department agent
- Notifications only on first assign, not on updates
- Escalation is in the title of the problem; not implemented
- Load balancing, tests, and deploy are not done
- `refresh_tokens` is unused
