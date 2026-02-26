# migration-auth API Reference

## 1. Overview

- Service: `migration-admin-api`
- Base path: `/api/v1/auth`
- Content type: `application/json`

### 1.1 Standard response envelope

All endpoints return this envelope:

| Field | Type | Description |
|---|---|---|
| code | integer | `200` means success; other values mean failure. |
| message | string | Human-readable message. |
| data | object or null | Endpoint payload. |

Success example:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 2. Authentication APIs

### 2.1 Login

- Method: `POST`
- Path: `/login`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| username | string | yes | Login username, max length `64`. |
| password | string | yes | Login password, max length `128`. |

Response `data`: `LoginResponse`

| Field | Type | Description |
|---|---|---|
| access_token | string | JWT token for request authentication. |
| user_info | UserInfo | Current user profile. |

`UserInfo` fields:

| Field | Type | Description |
|---|---|---|
| username | string | Login username. |
| display_name | string | Display name for UI. |

Success example:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "user_info": {
      "username": "admin",
      "display_name": "System Administrator"
    }
  }
}
```

### 2.2 Query current user

- Method: `GET`
- Path: `/query_current_user`
- Header: `Authorization: Bearer <access_token>`

Response `data`: `UserInfo`.

### 2.3 Logout

- Method: `POST`
- Path: `/logout`
- Header: `Authorization: Bearer <access_token>`

Request body: none.

Response: `Result<Void>` (`data` is `null` on success).

---

## 3. Front-end integration contract

### 3.1 Request headers

After login, front-end must include token in all API requests:

```http
Authorization: Bearer {access_token}
```

### 3.2 Login page behavior scope (admin-ui)

- Login form fields: `username`, `password`
- No "remember me"
- On successful login:
  - persist `access_token` (session-level)
  - cache `user_info`
  - route to intended page
- On logout:
  - clear `access_token` and `user_info`
  - route to `/login`

---

## 4. Scenario error responses

Error codes reference:

| code | message |
|---|---|
| 200 | success |
| 400 | bad request |
| 401 | unauthorized |
| 403 | forbidden |
| 404 | not found |
| 405 | method not allowed |
| 409 | conflict |
| 429 | too many requests |
| 10400 | parameter validation failed |
| 500 | internal server error |
| 503 | service unavailable |

Unauthorized example:

```json
{
  "code": 401,
  "message": "unauthorized",
  "data": null
}
```

Parameter validation failed example:

```json
{
  "code": 10400,
  "message": "parameter validation failed",
  "data": null
}
```
