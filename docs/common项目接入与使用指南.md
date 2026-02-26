# common 项目接入与使用指南

## 1. 依赖来源

本项目统一依赖外部公共库工程（本机路径）：

- `e:/data/Java/common/common-base`
- `e:/data/Java/common/common-springboot-middleware`

禁止在 `migration` 仓库内自行复制/创建同名模块。

---

## 2. Maven 依赖

```xml
<dependency>
    <groupId>top.bulgat</groupId>
    <artifactId>common-base</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>top.bulgat</groupId>
    <artifactId>common-springboot-middleware</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 3. common-base 核心用法

### 3.1 统一响应

- 使用：`top.bulgat.common.model.Result`
- 分页使用：`top.bulgat.common.model.PageResult`

示例：

```java
return Result.success(data);

PageResult<UserVO> pageResult = PageResult.of(current, size, total, list);
return Result.success(pageResult);
```

说明：`PageResult` 输出字段为 `current`、`size`、`total`、`list`；
`getTotalPages()` 与 `hasNext()` 使用 `@JsonIgnore`，不参与接口JSON输出。

### 3.2 业务异常

- 异常类型：`top.bulgat.common.exception.BizException`
- 错误码：`top.bulgat.common.exception.ErrorCode`

示例：

```java
if (task == null) {
    throw new BizException(ErrorCode.NOT_FOUND, "迁移任务不存在");
}
```

### 3.3 线程上下文与工具类

- Trace 上下文：`top.bulgat.common.thread.ThreadContext`
- JSON 工具：`top.bulgat.common.util.JsonUtils`
- 字符串工具：`top.bulgat.common.util.StringUtils`

---

## 4. common-springboot-middleware 核心用法

该模块通过 Spring Boot AutoConfiguration 自动生效，核心能力：

- 全局异常处理：`GlobalExceptionHandler`
- TraceId 过滤器：`TraceIdFilter`（请求头 `X-Trace-Id`）
- 异步线程 MDC 透传：`MdcAsyncConfig`
- Jackson 全局配置：`JacksonConfig`

可选请求日志开关：

```yaml
common:
  middleware:
    request-log:
      enabled: true
```

---

## 5. migration 项目落地约束

1. Controller 返回值统一使用 `top.bulgat.common.model.Result`
2. 列表分页统一使用 `top.bulgat.common.model.PageResult`
3. 不再在业务模块重复定义：
   - `Result`
   - `PageResult`
   - `GlobalExceptionHandler`
4. 异常优先抛 `BizException`，由 middleware 统一收敛

---

## 6. 版本与发布建议

- `migration` 依赖固定版本 `1.0.0`
- 公共库升级时，先在 `common` 仓库发布，再在 `migration` 统一升级版本
- 升级后必须执行全量回归（接口响应结构、错误码、分页结构）
