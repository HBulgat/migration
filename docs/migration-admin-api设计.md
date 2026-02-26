# migration-admin-api 模块设计

### 4.1 模块职责

提供管理后台REST API，负责：
- 迁移任务管理（推送到配置中心）
- 灰度规则管理（推送到配置中心，包含enable状态更新）
- Diff规则管理（推送到配置中心，包含enable状态更新）
- Diff结果记录查询

**设计理念**：迁移任务、灰度规则、Diff规则均由Admin API统一管理并存储在配置中心，Diff服务直接读取配置中心的Diff规则。只有Diff结果记录需要落库。

### 4.2 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | Web框架 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0 | 数据库 |
| Nacos | 2.2.0 | 配置中心 |
| Knife4j | 4.3.0 | API文档 |

### 4.3 数据库表设计

```sql
-- Diff结果记录表（唯一需要落库的表）
CREATE TABLE diff_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    migration_key VARCHAR(64) NOT NULL COMMENT '迁移任务key',
    trace_id VARCHAR(64) COMMENT '链路ID',
    old_response TEXT COMMENT '旧接口响应',
    new_response TEXT COMMENT '新接口响应',
    diff_results TEXT COMMENT 'Diff结果(JSON)',
    has_diff TINYINT NOT NULL DEFAULT 0 COMMENT '是否有差异(0-否/1-是)',
    diff_type VARCHAR(32) COMMENT '差异类型',
    grayscale_param VARCHAR(512) COMMENT '灰度参数',
    old_cost_time_ms INT COMMENT '旧接口耗时(ms)',
    new_cost_time_ms INT COMMENT '新接口耗时(ms)',
    total_cost_time_ms INT COMMENT '总耗时(ms)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_migration_key (migration_key),
    KEY idx_trace_id (trace_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Diff结果记录表';
```

### 4.4 API接口设计

```java
// 迁移任务API
@RestController
@RequestMapping("/api/v1/migration_task")
public class MigrationTaskController {

    /**
     * 创建迁移任务（推送到配置中心）
     * POST /api/v1/migration_task/create
     */
    @PostMapping("/create")
    public Result<MigrationTaskVO> create(@RequestBody MigrationTaskCreateRequest request);

    /**
     * 更新迁移任务（推送到配置中心）
     * POST /api/v1/migration_task/update
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody MigrationTaskUpdateRequest request);

    /**
     * 查询迁移任务（从配置中心拉取）
     * POST /api/v1/migration_task/query
     */
    @PostMapping("/query")
    public Result<MigrationTaskVO> query(@RequestBody MigrationTaskQueryRequest request);

    /**
     * 删除迁移任务（从配置中心删除）
     * POST /api/v1/migration_task/delete
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody MigrationTaskDeleteRequest request);

    /**
     * 获取迁移任务列表
     * GET /api/v1/migration_task/list
     */
    @GetMapping("/list")
    public Result<PageResult<MigrationTaskVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize);

    /**
     * 更新迁移状态（推送到配置中心）
     * POST /api/v1/migration_task/update_status
     */
    @PostMapping("/update_status")
    public Result<Void> updateStatus(@RequestBody UpdateStatusRequest request);
}

// 灰度规则API
@RestController
@RequestMapping("/api/v1/grayscale_rule")
public class GrayscaleRuleController {

    /**
     * 创建灰度规则（推送到配置中心）
     */
    @PostMapping("/create")
    public Result<GrayscaleRuleVO> create(@RequestBody GrayscaleRuleCreateRequest request);

    /**
     * 更新灰度规则（推送到配置中心）
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody GrayscaleRuleUpdateRequest request);

    /**
     * 删除灰度规则（从配置中心删除）
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody GrayscaleRuleDeleteRequest request);

    /**
     * 更新灰度规则启用状态（推送到配置中心）
     */
    @PostMapping("/update_enable")
    public Result<Void> updateEnable(@RequestBody UpdateGrayscaleRuleEnableRequest request);

    /**
     * 获取灰度规则列表（从配置中心拉取，分页）
     */
    @GetMapping("/list")
    public Result<PageResult<GrayscaleRuleVO>> list(
            @RequestParam String migration_key,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize);
}

// Diff结果记录API
@RestController
@RequestMapping("/api/v1/diff_record")
public class DiffRecordController {

    /**
     * 分页查询Diff记录
     */
    @GetMapping("/list")
    public Result<PageResult<DiffRecordVO>> list(
            @RequestParam String migration_key,
            @RequestParam(required = false) Integer has_diff,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize);

    /**
     * 查询Diff记录详情
     */
    @GetMapping("/detail")
    public Result<DiffRecordVO> detail(@RequestParam Long id);

    /**
     * 统计Diff结果
     */
    @GetMapping("/statistics")
    public Result<DiffStatisticsVO> statistics(@RequestParam String migration_key,
                                                @RequestParam(required = false) Date startDate,
                                                @RequestParam(required = false) Date endDate);
}
```

### 4.5 请求/响应VO设计

```java
// 创建迁移任务请求
@Data
@Builder
public class MigrationTaskCreateRequest {
    @NotBlank
    private String migrationKey;

    @NotNull
    private Integer status;      // 默认1

    private String description;
}

// 更新灰度规则启用状态请求
@Data
@Builder
public class UpdateGrayscaleRuleEnableRequest {
    @NotBlank
    private String migrationKey;

    @NotBlank
    private String ruleId;       // 规则ID

    @NotNull
    private Boolean enable;
}

// Diff记录VO
@Data
@Builder
public class DiffRecordVO {
    private Long id;
    private String migrationKey;
    private String traceId;
    private String oldResponse;
    private String newResponse;
    private List<DiffItemVO> diffResults;
    private Boolean hasDiff;
    private String diffType;
    private String grayscaleParam;
    private Integer oldCostTimeMs;
    private Integer newCostTimeMs;
    private Integer totalCostTimeMs;
    private Date createTime;
}

// Diff统计VO
@Data
@Builder
public class DiffStatisticsVO {
    private Long totalCount;
    private Long diffCount;
    private Double diffRate;
    private Integer avgOldCostTime;
    private Integer avgNewCostTime;
}
```

### 4.6 配置中心推送服务

```java
@Service
public class ConfigCenterService {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    /**
     * 推送迁移配置到配置中心
     */
    public void pushMigrationConfig(MigrationConfig config) {
        String dataId = "migration_" + config.getMigrationKey();
        String group = "DEFAULT_GROUP";
        String content = JSON.toJSONString(config);
        nacosConfigManager.publishConfig(dataId, group, content);
    }

    /**
     * 推送灰度规则到配置中心
     */
    public void pushGrayscaleRules(String migrationKey, List<GrayscaleConfig> rules) {
        String dataId = "grayscale_" + migrationKey;
        String group = "DEFAULT_GROUP";
        String content = JSON.toJSONString(rules);
        nacosConfigManager.publishConfig(dataId, group, content);
    }

    /**
     * 从配置中心拉取配置
     */
    public String getConfig(String dataId, String group) {
        return nacosConfigManager.getConfig(dataId, group, 5000);
    }
}
```

### 4.7 模块依赖

```
migration-admin-api
├── spring-boot-starter-web
├── mybatis-plus
├── mysql-connector-java
├── knife4j-spring-boot-starter
├── nacos-client
└── fastjson2
```

### 4.8 DDD分层架构（强制）

`migration-admin-api` 按 DDD 四层组织代码，禁止跨层直接调用：

```
top.bulgat.migration.admin
├── interfaces/                  # 接口层（Controller、DTO、Assembler）
│   ├── rest/
│   ├── dto/
│   └── assembler/
├── application/                 # 应用层（用例编排、事务、命令/查询服务）
│   ├── command/
│   ├── query/
│   └── service/
├── domain/                      # 领域层（实体、值对象、领域服务、仓储接口）
│   ├── model/
│   ├── service/
│   ├── repository/
│   └── event/
└── infrastructure/              # 基础设施层（仓储实现、Nacos/MySQL适配）
    ├── persistence/
    ├── configcenter/
    ├── repository/
    └── convert/
```

约束说明：
- Controller 只接收/返回 DTO，不直接操作持久化对象。
- 应用层负责流程编排（如“创建任务+推送配置中心”），不承载规则判断细节。
- 灰度规则校验、状态流转规则放在领域层（聚合根/领域服务）。
- 配置中心与数据库访问全部下沉到 infrastructure，通过 domain 仓储接口向上暴露能力。

---


### 4.9 查询参数约束补充

- `GET /api/v1/diff_record/list`
- `GET /api/v1/diff_record/statistics`

当同时传入 `start_date` 和 `end_date` 时，必须满足：`start_date <= end_date`。  
若不满足，接口返回统一业务错误码（`PARAM_ERROR`）。
- `GET /api/v1/migration_task/list` 的 `status` 过滤值必须在 `[1,7]`，否则返回 `PARAM_ERROR`。
- `GET /api/v1/diff_record/list` 的 `has_diff` 过滤值仅允许 `0/1`，否则返回 `PARAM_ERROR`。
- `GET /api/v1/migration_task/list`、`GET /api/v1/grayscale_rule/list`、`GET /api/v1/diff_record/list` 的分页参数 `page/pageSize` 必须满足 `page >= 1` 且 `1 <= pageSize <= 200`，否则返回 `PARAM_ERROR`。
- `POST /api/v1/migration_task/update` 至少传入一个更新字段：`status` 或 `description`。
- `POST /api/v1/grayscale_rule/update` 至少传入一个更新字段：`rule_type`、`rule_value`、`enable`。
