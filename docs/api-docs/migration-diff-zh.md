# migration-diff 接口文档（中文版）

> 说明：这是新增的中文版接口文档，字段与约束与英文版保持一致。


## 1. 概览

- 服务：`migration-diff`
- 基础路径: `/api/v1`
- 请求类型: `application/json`

### 1.1 统一响应结构

| 字段 | 类型 | 说明 |
|---|---|---|
| code | integer | `0` for success; validation errors are usually `400`; business errors use business codes (for example `10400`). |
| message | string | Human-readable message. |
| data | object or null | Endpoint payload. |

成功示例:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

---

## 2. Diff执行接口

- 方法: `POST`
- 路径: `/api/v1/diff`

### 2.1 请求体

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 必填, max length `128`, 否 whitespace. |
| trace_id | string | 否 | Trace id. |
| old_json | string | 是 | Old response JSON string. |
| new_json | string | 是 | New response JSON string. |
| old_cost_time_ms | integer | 否 | Must be `>= 0` when provided. |
| new_cost_time_ms | integer | 否 | Must be `>= 0` when provided. |
| grayscale_param | string | 否 | Grayscale params, stored as-is. |

请求示例：

```json
{
  "migration_key": "user.query",
  "trace_id": "trace-123",
  "old_json": "{\"name\":\"tom\",\"age\":18}",
  "new_json": "{\"name\":\"tom\",\"age\":19}",
  "old_cost_time_ms": 12,
  "new_cost_time_ms": 10,
  "grayscale_param": "{\"uid\":1001}"
}
```

### 2.2 响应 data

`data` is `DiffExecute响应`:

| 字段 | 类型 | 说明 |
|---|---|---|
| has_diff | boolean | 是否有差异。 |
| diff_results | array<DiffItem响应> | 差异明细列表。 |
| cost_time_ms | long | Diff计算耗时(ms)。 |

`DiffItem响应` fields:

| 字段 | 类型 | 说明 |
|---|---|---|
| path | string | Diff path, for example `user.name` or `items[0].price`. |
| old_value | string | Old value. |
| new_value | string | New value. |
| diff_type | string | `ADD` / `REMOVE` / `MODIFY`. |

成功示例:

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

## 3. 运行时规则行为

Rules are loaded by `migration_key` from config center, and enabled rules are applied in this service.

支持的规则类型：
- `IGNORE`
- `TOLERANCE`
- `SCRIPT` (SpEL)
- `SORT` (sort arrays by configured field before comparing)

单条规则非法时，会被忽略，Diff仍会继续执行。

---

## 4. 常见错误响应

参数校验失败示例:

```json
{
  "code": 400,
  "message": "Validation failed: must 否t be blank",
  "data": null
}
```

业务错误示例:

```json
{
  "code": 10400,
  "message": "invalid diff payload",
  "data": null
}
```

常见 message：
- `diff command is required`
- `migration_key is required`
- `migration_key is too long`
- `migration_key must 否t contain space`
- `old_json is required`
- `new_json is required`
- `old_cost_time_ms must be greater than or equal to 0`
- `new_cost_time_ms must be greater than or equal to 0`
- `invalid diff payload`