# migration-admin-api API Reference

## 1. Overview

- Service: `migration-admin-api`
- Base path: `/api/v1`
- Content type: `application/json`

### 1.1 Standard response envelope

All endpoints return this envelope:

| Field | Type | Description |
|---|---|---|
| code | integer | `0` for success; validation errors are usually `400`; business errors use business codes (for example `10400`, `404`). |
| message | string | Human-readable message. |
| data | object or null | Endpoint payload. |

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 1.2 PageResult payload

List APIs return `PageResult<T>` in `data`:

| Field | Type | Description |
|---|---|---|
| current | integer | Current page number. |
| size | integer | Page size. |
| total | long | Total records. |
| list | array<T> | Page data list. |

### 1.3 Common validation notes

For request-body field `migration_key`:
- required
- max length `128`
- no whitespace characters

For paging query params:
- `page >= 1`
- `1 <= page_size <= 200`

---

## 2. Migration Task APIs

Base path: `/api/v1/migration_task`

### 2.1 Create task

- Method: `POST`
- Path: `/create`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Unique task key. |
| status | integer | yes | Status code in `1..7`. |
| description | string | no | Task description. |

Response `data`: `MigrationTaskResponse`

| Field | Type | Description |
|---|---|---|
| migration_key | string | Task key. |
| status | integer | Current status code. |
| description | string | Task description. |
| create_time | string(datetime) | Creation time. |
| update_time | string(datetime) | Last update time. |

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "migration_key": "user.query",
    "status": 1,
    "description": "query old API",
    "create_time": "2026-02-24T10:00:00",
    "update_time": "2026-02-24T10:00:00"
  }
}
```

### 2.2 Update task

- Method: `POST`
- Path: `/update`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| status | integer | no | New status code in `1..7`. |
| description | string | no | New description. |

Business rule: at least one of `status` and `description` must be provided.

Response: `Result<Void>` (`data` is `null` on success).

### 2.3 Query task

- Method: `POST`
- Path: `/query`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |

Response `data`: `MigrationTaskResponse`.

### 2.4 Delete task

- Method: `POST`
- Path: `/delete`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |

Response: `Result<Void>`.

### 2.5 List tasks

- Method: `GET`
- Path: `/list`

Query params:

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| page | integer | no | `1` | Page number. |
| page_size | integer | no | `10` | Page size. |
| status | integer | no | - | Filter by status (`1..7`). |
| keyword | string | no | - | Fuzzy match by `migration_key`. |

Response `data`: `PageResult<MigrationTaskResponse>`.

### 2.6 Update task status

- Method: `POST`
- Path: `/update_status`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| target_status | integer | yes | Target status in `1..7`. |

Status transition rule:
- same status is allowed
- rollback is allowed
- forward transition must be exactly `+1`

Response: `Result<Void>`.

---

## 3. Grayscale Rule APIs

Base path: `/api/v1/grayscale_rule`

### 3.1 Create rule

- Method: `POST`
- Path: `/create`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| rule_type | string | yes | `PERCENTAGE` / `BLACKLIST` / `WHITELIST` / `EXPRESSION`. |
| rule_value | string | yes | Rule value. |
| enable | boolean | yes | Enabled flag. |

`rule_value` constraints:
- `PERCENTAGE`: integer in `[0,100]`
- `BLACKLIST` and `WHITELIST`: JSON array string
- `EXPRESSION`: non-empty string

Response `data`: `GrayscaleRuleResponse`

| Field | Type | Description |
|---|---|---|
| rule_id | string | Rule id. |
| migration_key | string | Task key. |
| rule_type | string | Rule type. |
| rule_value | string | Rule value. |
| enable | boolean | Enabled flag. |
| create_time | string(datetime) | Creation time. |
| update_time | string(datetime) | Last update time. |

### 3.2 Update rule

- Method: `POST`
- Path: `/update`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| rule_id | string | yes | Rule id. |
| rule_type | string | no | New rule type. |
| rule_value | string | no | New rule value. |
| enable | boolean | no | New enabled flag. |

Business rule: at least one of `rule_type`, `rule_value`, `enable` must be provided.

Response: `Result<Void>`.

### 3.3 Delete rule

- Method: `POST`
- Path: `/delete`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| rule_id | string | yes | Rule id. |

Response: `Result<Void>`.

### 3.4 Update rule enable

- Method: `POST`
- Path: `/update_enable`

Request body:

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| rule_id | string | yes | Rule id. |
| enable | boolean | yes | Enabled flag. |

Response: `Result<Void>`.

### 3.5 List rules

- Method: `GET`
- Path: `/list`

Query params:

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| migration_key | string | yes | - | Task key. |
| page | integer | no | `1` | Page number. |
| page_size | integer | no | `10` | Page size. |

Response `data`: `PageResult<GrayscaleRuleResponse>`.

---

## 4. Diff Record Query APIs

Base path: `/api/v1/diff_record`

### 4.1 List diff records

- Method: `GET`
- Path: `/list`

Query params:

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| migration_key | string | yes | - | Task key. |
| has_diff | integer | no | - | `0` or `1`. |
| start_date | string(date) | no | - | `yyyy-MM-dd`. |
| end_date | string(date) | no | - | `yyyy-MM-dd`. |
| page | integer | no | `1` | Page number. |
| page_size | integer | no | `10` | Page size. |

Business rule: if both dates are provided, `start_date <= end_date`.

Response `data`: `PageResult<DiffRecordResponse>`.

`DiffRecordResponse` fields:

| Field | Type | Description |
|---|---|---|
| id | long | Record id. |
| migration_key | string | Task key. |
| trace_id | string | Trace id. |
| old_response | string | Old response body. |
| new_response | string | New response body. |
| diff_results | array<DiffItemResponse> | Diff details. |
| has_diff | boolean | Whether diff exists. |
| diff_type | string | Diff summary type. |
| grayscale_param | string | Grayscale params. |
| old_cost_time_ms | integer | Old api cost in ms. |
| new_cost_time_ms | integer | New api cost in ms. |
| total_cost_time_ms | integer | Total cost in ms. |
| create_time | string(datetime) | Creation time. |

`DiffItemResponse` fields:

| Field | Type | Description |
|---|---|---|
| field_path | string | Field path. |
| old_value | string | Old value. |
| new_value | string | New value. |
| diff_type | string | `ADD` / `REMOVE` / `MODIFY`. |

### 4.2 Diff record detail

- Method: `GET`
- Path: `/detail`

Query params:

| Param | Type | Required | Description |
|---|---|---|---|
| id | long | yes | Record id. |

Response `data`: `DiffRecordResponse`.

### 4.3 Diff statistics

- Method: `GET`
- Path: `/statistics`

Query params:

| Param | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Task key. |
| start_date | string(date) | no | `yyyy-MM-dd`. |
| end_date | string(date) | no | `yyyy-MM-dd`. |

Business rule: if both dates are provided, `start_date <= end_date`.

Response `data`: `DiffStatisticsResponse`

| Field | Type | Description |
|---|---|---|
| total_count | long | Total records. |
| diff_count | long | Records with diff. |
| diff_rate | double | Diff rate in `[0,1]`. |
| avg_old_cost_time | int | Avg old api cost. |
| avg_new_cost_time | int | Avg new api cost. |

---

## 5. Typical error responses

Validation error example:

```json
{
  "code": 400,
  "message": "Validation failed: must not be blank",
  "data": null
}
```

Business error example:

```json
{
  "code": 10400,
  "message": "start_date must not be later than end_date",
  "data": null
}
```

Common messages:
- `page must be greater than or equal to 1`
- `pageSize out of range [1,200]`
- `status out of range [1,7]`
- `start_date must not be later than end_date`
- `migration task not found: ...`
- `grayscale rule not found: ...`
- `diff record not found: ...`
- `migration_key is required`
- `migration_key is too long`
- `migration_key must not contain space`
