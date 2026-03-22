# migration-admin-api 接口文档（中文版）

> 说明：这是新增的中文版接口文档，字段与约束与英文版保持一致。


## 1. 概览

- 服务：`migration-admin-api`
- 基础路径: `/api/v1`
- 请求类型: `application/json`

### 1.1 统一响应结构

所有接口都返回如下包装结构：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | integer | `0` for success; validation errors are usually `400`; business errors use business codes (for example `10400`, `404`). |
| message | string | Human-readable message. |
| data | object or null | Endpoint payload. |

成功示例:

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 1.2 分页对象

List APIs return `PageResult<T>` in `data`:

| 字段 | 类型 | 说明 |
|---|---|---|
| current | integer | Current page number. |
| size | integer | 每页条数。 |
| total | long | Total records. |
| list | array<T> | Page data list. |

### 1.3 通用校验说明

For request-body field `migration_key`:
- required
- max length `128`
- 否 whitespace characters

For paging query params:
- `page >= 1`
- `1 <= page_size <= 200`

---

## 2. 迁移任务接口

基础路径: `/api/v1/migration_task`

### 2.1 创建任务

- 方法: `POST`
- 路径: `/create`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | Unique task key. |
| status | integer | 是 | Status code in `1..7`. |
| description | string | 否 | 任务描述。 |

响应 `data`: `MigrationTask响应`

| 字段 | 类型 | 说明 |
|---|---|---|
| migration_key | string | 迁移任务 key。 |
| status | integer | Current status code. |
| description | string | 任务描述。 |
| create_time | string(datetime) | 创建时间。 |
| update_time | string(datetime) | 更新时间。 |

成功示例:

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

### 2.2 更新任务

- 方法: `POST`
- 路径: `/update`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| status | integer | 否 | New status code in `1..7`. |
| description | string | 否 | New description. |

Business rule: at least one of `status` and `description` must be provided.

响应: `Result<Void>` (`data` is `null` on success).

### 2.3 查询任务

- 方法: `POST`
- 路径: `/query`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |

响应 `data`: `MigrationTask响应`.

### 2.4 删除任务

- 方法: `POST`
- 路径: `/delete`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |

响应: `Result<Void>`.

### 2.5 任务分页列表

- 方法: `GET`
- 路径: `/list`

Query参数:

| Param | 类型 | 必填 | Default | 说明 |
|---|---|---|---|---|
| page | integer | 否 | `1` | 页码。 |
| page_size | integer | 否 | `10` | 每页条数。 |
| status | integer | 否 | - | Filter by status (`1..7`). |
| keyword | string | 否 | - | Fuzzy match by `migration_key`. |

响应 `data`: `PageResult<MigrationTask响应>`.

### 2.6 更新任务 status

- 方法: `POST`
- 路径: `/update_status`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| target_status | integer | 是 | Target status in `1..7`. |

Status transition rule:
- same status is allowed
- rollback is allowed
- forward transition must be exactly `+1`

响应: `Result<Void>`.

---

## 3. 灰度规则接口

基础路径: `/api/v1/gray_rule`

### 3.1 创建灰度规则

- 方法: `POST`
- 路径: `/create`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| rule_type | string | 是 | `PERCENTAGE` / `BLACKLIST` / `WHITELIST` / `EXPRESSION`. |
| rule_value | string | 是 | Rule value. |
| enable | boolean | 是 | Enabled flag. |

`rule_value` constraints:
- `PERCENTAGE`: integer in `[0,100]`
- `BLACKLIST` and `WHITELIST`: JSON array string
- `EXPRESSION`: 否n-empty string

响应 `data`: `GrayRule响应`

| 字段 | 类型 | 说明 |
|---|---|---|
| rule_id | string | Rule id. |
| migration_key | string | 迁移任务 key。 |
| rule_type | string | Rule type. |
| rule_value | string | Rule value. |
| enable | boolean | Enabled flag. |
| create_time | string(datetime) | 创建时间。 |
| update_time | string(datetime) | 更新时间。 |

### 3.2 更新灰度规则

- 方法: `POST`
- 路径: `/update`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| rule_id | string | 是 | Rule id. |
| rule_type | string | 否 | New rule type. |
| rule_value | string | 否 | New rule value. |
| enable | boolean | 否 | New enabled flag. |

Business rule: at least one of `rule_type`, `rule_value`, `enable` must be provided.

响应: `Result<Void>`.

### 3.3 删除灰度规则

- 方法: `POST`
- 路径: `/delete`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| rule_id | string | 是 | Rule id. |

响应: `Result<Void>`.

### 3.4 更新灰度规则 enable

- 方法: `POST`
- 路径: `/update_enable`

请求体:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| rule_id | string | 是 | Rule id. |
| enable | boolean | 是 | Enabled flag. |

响应: `Result<Void>`.

### 3.5 规则分页列表

- 方法: `GET`
- 路径: `/list`

Query参数:

| Param | 类型 | 必填 | Default | 说明 |
|---|---|---|---|---|
| migration_key | string | 是 | - | 迁移任务 key。 |
| page | integer | 否 | `1` | 页码。 |
| page_size | integer | 否 | `10` | 每页条数。 |

响应 `data`: `PageResult<GrayRule响应>`.

---

## 4. Diff记录查询接口

基础路径: `/api/v1/diff_record`

### 4.1 Diff记录分页列表

- 方法: `GET`
- 路径: `/list`

Query参数:

| Param | 类型 | 必填 | Default | 说明 |
|---|---|---|---|---|
| migration_key | string | 是 | - | 迁移任务 key。 |
| has_diff | integer | 否 | - | `0` or `1`. |
| start_date | string(date) | 否 | - | `yyyy-MM-dd`. |
| end_date | string(date) | 否 | - | `yyyy-MM-dd`. |
| page | integer | 否 | `1` | 页码。 |
| page_size | integer | 否 | `10` | 每页条数。 |

Business rule: if both dates are provided, `start_date <= end_date`.

响应 `data`: `PageResult<DiffRecord响应>`.

`DiffRecord响应` fields:

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 记录ID。 |
| migration_key | string | 迁移任务 key。 |
| trace_id | string | 链路ID。 |
| old_response | string | Old response body. |
| new_response | string | New response body. |
| diff_results | array<DiffItem响应> | Diff details. |
| has_diff | boolean | Whether diff exists. |
| diff_type | string | Diff summary type. |
| gray_param | string | Gray params. |
| old_cost_time_ms | integer | Old api cost in ms. |
| new_cost_time_ms | integer | New api cost in ms. |
| total_cost_time_ms | integer | Total cost in ms. |
| create_time | string(datetime) | 创建时间。 |

`DiffItem响应` fields:

| 字段 | 类型 | 说明 |
|---|---|---|
| field_path | string | 字段 path. |
| old_value | string | Old value. |
| new_value | string | New value. |
| diff_type | string | `ADD` / `REMOVE` / `MODIFY`. |

### 4.2 Diff记录详情

- 方法: `GET`
- 路径: `/detail`

Query参数:

| Param | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 记录ID。 |

响应 `data`: `DiffRecord响应`.

### 4.3 Diff统计

- 方法: `GET`
- 路径: `/statistics`

Query参数:

| Param | 类型 | 必填 | 说明 |
|---|---|---|---|
| migration_key | string | 是 | 迁移任务 key。 |
| start_date | string(date) | 否 | `yyyy-MM-dd`. |
| end_date | string(date) | 否 | `yyyy-MM-dd`. |

Business rule: if both dates are provided, `start_date <= end_date`.

响应 `data`: `DiffStatistics响应`

| 字段 | 类型 | 说明 |
|---|---|---|
| total_count | long | Total records. |
| diff_count | long | Records with diff. |
| diff_rate | double | Diff rate in `[0,1]`. |
| avg_old_cost_time | int | Avg old api cost. |
| avg_new_cost_time | int | Avg new api cost. |

---

## 5. 常见错误响应

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
  "message": "start_date must 否t be later than end_date",
  "data": null
}
```

常见 message：
- `page must be greater than or equal to 1`
- `pageSize out of range [1,200]`
- `status out of range [1,7]`
- `start_date must 否t be later than end_date`
- `migration task 否t found: ...`
- `gray rule 否t found: ...`
- `diff record 否t found: ...`
- `migration_key is required`
- `migration_key is too long`
- `migration_key must 否t contain space`