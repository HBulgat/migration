# migration-diff API Reference

## 1. Overview

- Service: `migration-diff`
- Base path: `/api/v1`
- Content type: `application/json`

### 1.1 Standard response envelope

| Field | Type | Description |
|---|---|---|
| code | integer | `0` for success; validation errors are usually `400`; business errors use business codes (for example `10400`). |
| message | string | Human-readable message. |
| data | object or null | Endpoint payload. |

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

---

## 2. Execute Diff API

- Method: `POST`
- Path: `/api/v1/diff`

### 2.1 Request body

| Field | Type | Required | Description |
|---|---|---|---|
| migration_key | string | yes | Required, max length `128`, no whitespace. |
| trace_id | string | no | Trace id. |
| old_json | string | yes | Old response JSON string. |
| new_json | string | yes | New response JSON string. |
| old_cost_time_ms | integer | no | Must be `>= 0` when provided. |
| new_cost_time_ms | integer | no | Must be `>= 0` when provided. |
| gray_param | string | no | Gray params, stored as-is. |

Request example:

```json
{
  "migration_key": "user.query",
  "trace_id": "trace-123",
  "old_json": "{\"name\":\"tom\",\"age\":18}",
  "new_json": "{\"name\":\"tom\",\"age\":19}",
  "old_cost_time_ms": 12,
  "new_cost_time_ms": 10,
  "gray_param": "{\"uid\":1001}"
}
```

### 2.2 Response data

`data` is `DiffExecuteResponse`:

| Field | Type | Description |
|---|---|---|
| has_diff | boolean | Whether diff exists. |
| diff_results | array<DiffItemResponse> | Diff detail list. |
| cost_time_ms | long | Diff calculation cost in ms. |

`DiffItemResponse` fields:

| Field | Type | Description |
|---|---|---|
| path | string | Diff path, for example `user.name` or `items[0].price`. |
| old_value | string | Old value. |
| new_value | string | New value. |
| diff_type | string | `ADD` / `REMOVE` / `MODIFY`. |

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "has_diff": true,
    "diff_results": [
      {
        "path": "age",
        "old_value": "18",
        "new_value": "19",
        "diff_type": "MODIFY"
      }
    ],
    "cost_time_ms": 5
  }
}
```

---

## 3. Rule behavior at runtime

Rules are loaded by `migration_key` from config center, and enabled rules are applied in this service.

Supported rule types:
- `IGNORE`
- `TOLERANCE`
- `SCRIPT` (SpEL)
- `SORT` (sort arrays by configured field before comparing)

If a rule item is invalid, it is skipped and diff execution continues.

---

## 4. Typical error responses

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
  "message": "invalid diff payload",
  "data": null
}
```

Common messages:
- `diff command is required`
- `migration_key is required`
- `migration_key is too long`
- `migration_key must not contain space`
- `old_json is required`
- `new_json is required`
- `old_cost_time_ms must be greater than or equal to 0`
- `new_cost_time_ms must be greater than or equal to 0`
- `invalid diff payload`
