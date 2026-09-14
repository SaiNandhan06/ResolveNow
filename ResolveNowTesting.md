# ResolveNow API Testing

Run these requests in order. Replace `<accessToken>` with the token returned by registration or login.

## 1. Register

```http
POST http://localhost:9080/api/auth/register
Content-Type: application/json

{
  "username": "asha",
  "email": "asha@example.com",
  "password": "Passw0rd!"
}
```

Expected: `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "b6e1b6...-uuid",
  "role": "CUSTOMER",
  "userId": 1
}
```

## 2. Login (optional)

```http
POST http://localhost:9080/api/auth/login
Content-Type: application/json

{
  "username": "asha",
  "password": "Passw0rd!"
}
```

Expected: `200 OK`, with the same response shape as registration.

## 3. Create a complaint

```http
POST http://localhost:9080/api/complaints
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "title": "Invoice mismatch",
  "description": "My invoice shows a wrong amount for this month",
  "category": "BILLING"
}
```

Expected: `200 OK`

```json
{
  "complaintId": 1,
  "userId": 1,
  "title": "Invoice mismatch",
  "description": "My invoice shows a wrong amount for this month",
  "category": "BILLING",
  "status": "OPEN",
  "createdAt": "2026-09-13T20:10:00"
}
```

## 4. Get the complaint

```http
GET http://localhost:9080/api/complaints/1
Authorization: Bearer <accessToken>
```

Expected: `200 OK`

```json
{
  "complaintId": 1,
  "userId": 1,
  "title": "Invoice mismatch",
  "description": "My invoice shows a wrong amount for this month",
  "category": "BILLING",
  "status": "ASSIGNED",
  "createdAt": "2026-09-13T20:10:00"
}
```

## 5. Get the assignment

```http
GET http://localhost:9080/api/assignments/1
Authorization: Bearer <accessToken>
```

Expected: `200 OK`

```json
{
  "assignmentId": 1,
  "complaintId": 1,
  "department": "BILLING",
  "assignedTo": 1,
  "status": "ASSIGNED",
  "assignedAt": "2026-09-13T20:10:01"
}
```

## 6. Get user notifications

```http
GET http://localhost:9080/api/notifications/user/1
Authorization: Bearer <accessToken>
```

Expected: `200 OK`

```json
[
  {
    "notificationId": 1,
    "userId": 1,
    "complaintId": 1,
    "message": "Your complaint has been assigned to BILLING",
    "read": false,
    "createdAt": "2026-09-13T20:10:01"
  }
]
```

## 7. Mark the notification as read

```http
PUT http://localhost:9080/api/notifications/1/read
Authorization: Bearer <accessToken>
```

Expected: `200 OK`, empty response body.

Repeat step 6 to verify that `read` is now `true`.
