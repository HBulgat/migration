# migration-diff 模块设计

### 5.1 模块职责

提供Diff比对服务，负责：
- 从配置中心拉取Diff规则
- 接收SDK发来的新旧接口响应
- 根据Diff规则执行比对
- 存储Diff结果到数据库

### 5.2 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | Web框架 |
| Jackson | 2.16.0 | JSON处理 |
| SpEL | Spring Expression | 表达式脚本执行 |
| zjsonpatch | 0.4.16 | JSON Patch生成(RFC 6902) |
| json-path | 2.9.0 | JSON Path查询 |
| MySQL | 8.0 | Diff结果存储 |
| MyBatis-Plus | 3.5.5 | ORM框架 |

### 5.3 核心请求/响应设计

```java
// Diff请求（由SDK异步发送）
@Data
@Builder
public class DiffRequest {
    private String migrationKey;    // 迁移任务key
    private String traceId;         // 链路ID
    private String oldJson;         // 旧接口响应
    private String newJson;         // 新接口响应
    private Integer oldCostTimeMs;  // 旧接口调用耗时
    private Integer newCostTimeMs;  // 新接口调用耗时
}

// Diff规则（从配置中心拉取）
@Data
@Builder
public class DiffRule {
    private String migrationKey;    // 迁移任务key
    private String ruleType;        // IGNORE/TOLERANCE/SCRIPT/SORT
    private String fieldPath;       // 字段路径（支持JSONPath）
    private String ruleValue;       // 规则值
    private Boolean enable;        // 是否启用
}

// Diff结果
@Data
@Builder
public class DiffResult {
    private Boolean hasDiff;              // 是否有差异
    private List<DiffItem> diffResults;   // 差异列表
    private Long costTimeMs;              // 耗时
}

// Diff差异项
@Data
@Builder
public class DiffItem {
    private String fieldPath;       // 字段路径
    private String oldValue;        // 旧值
    private String newValue;        // 新值
    private DiffType diffType;      // 差异类型(MODIFY/ADD/REMOVE)
}
```

### 5.4 Diff比对逻辑设计

#### 5.4.1 技术选型

| 技术 | 说明 |
|------|------|
| **zjsonpatch** | 根据RFC 6902生成JSON Patch，支持add/remove/replace/move/copy等操作 |
| **json-path** | JSON路径查询，用于匹配规则中的fieldPath |
| **Jackson** | JSON解析和序列化 |

**技术方案**：
1. 使用 **zjsonpatch** 生成JSON Patch（RFC 6902标准），自动识别ADD/REMOVE/MODIFY
2. 使用 **json-path** 匹配Diff规则中的fieldPath表达式
3. 支持IGNORE/TOLERANCE/SCRIPT/SORT四种规则类型（SORT用于数组排序预处理）

#### 5.4.2 核心比对流程

```
1. 接收DiffRequest
2. 从配置中心拉取Diff规则: migration_{key}
3. 按SORT规则先对数组排序
4. 使用zjsonpatch生成JSON Patch
5. 遍历Patch，应用规则过滤（IGNORE/TOLERANCE/SCRIPT）
6. 保存DiffRecord到数据库
7. 返回DiffResult
```

#### 5.4.3 核心比对逻辑

```java
@Service
public class DiffService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigCenterService configCenterService;

    /**
     * 执行Diff比对
     */
    public DiffResult execute(DiffRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 解析JSON
            JsonNode sourceNode = objectMapper.readTree(request.getOldJson());
            JsonNode targetNode = objectMapper.readTree(request.getNewJson());

            // 2. 从配置中心拉取Diff规则
            List<DiffRule> rules = configCenterService.getDiffRules(request.getMigrationKey());

            // 3. 使用zjsonpatch生成Patch
            JsonNode patch = JsonDiff.asJson(sourceNode, targetNode);
            List<JsonNode> patchElements = new ArrayList<>();
            if (patch.isArray()) {
                for (JsonNode element : patch) {
                    patchElements.add(element);
                }
            }

            // 4. 应用规则过滤
            List<DiffItem> diffItems = applyRules(patchElements, rules);

            // 5. 保存到数据库
            saveDiffRecord(request, diffItems);

            long costTime = System.currentTimeMillis() - startTime;

            return DiffResult.builder()
                    .hasDiff(!diffItems.isEmpty())
                    .diffResults(diffItems)
                    .costTimeMs(costTime)
                    .build();
        } catch (Exception e) {
            log.error("Diff execute failed", e);
            throw new RuntimeException("Diff execute failed", e);
        }
    }

    /**
     * 应用规则过滤
     */
    private List<DiffItem> applyRules(List<JsonNode> patchElements, List<DiffRule> rules) {
        List<DiffItem> result = new ArrayList<>();

        for (JsonNode patchNode : patchElements) {
            String op = patchNode.get("op").asText();
            String path = patchNode.get("path").asText();
            String oldValue = patchNode.has("from") ?
                patchNode.get("from").asText() : patchNode.has("value") ?
                objectMapper.valueToTree(patchNode.get("value")).toString() : null;
            String newValue = patchNode.has("value") ?
                patchNode.get("value").toString() : null;

            // 查找匹配的规则
            DiffRule matchingRule = findMatchingRule(path, rules);

            if (matchingRule == null) {
                // 无匹配规则，全部报告
                result.add(DiffItem.builder()
                        .fieldPath(path)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .diffType(convertOpToDiffType(op))
                        .build());
            } else {
                // 根据规则类型决定是否报告
                DiffRuleExecutor executor = ruleExecutorRegistry.getExecutor(matchingRule.getRuleType());
                DiffItem item = DiffItem.builder()
                        .fieldPath(path)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .diffType(convertOpToDiffType(op))
                        .build();

                if (executor.shouldReport(item, matchingRule)) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * 查找匹配的规则（支持json-path表达式匹配）
     */
    private DiffRule findMatchingRule(String path, List<DiffRule> rules) {
        for (DiffRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnable())) {
                continue;
            }
            String rulePath = rule.getFieldPath();
            // 使用json-path匹配
            if (matchesJsonPath(path, rulePath)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 使用json-path匹配路径
     */
    private boolean matchesJsonPath(String actualPath, String rulePath) {
        // 简单实现：支持精确匹配和通配符
        if (rulePath.equals(actualPath)) {
            return true;
        }
        if (rulePath.endsWith(".*")) {
            String prefix = rulePath.substring(0, rulePath.length() - 2);
            return actualPath.startsWith(prefix);
        }
        if (rulePath.equals("*")) {
            return true;
        }
        return false;
    }

    /**
     * 将Patch操作转换为DiffType
     */
    private DiffType convertOpToDiffType(String op) {
        return switch (op) {
            case "add" -> DiffType.ADD;
            case "remove" -> DiffType.REMOVE;
            case "replace", "move", "copy" -> DiffType.MODIFY;
            default -> DiffType.MODIFY;
        };
    }
}
```

#### 5.4.4 规则执行器设计

```java
// 规则执行器接口
public interface DiffRuleExecutor {
    /**
     * 是否需要报告差异
     */
    boolean shouldReport(DiffItem item, DiffRule rule);
}

// IGNORE执行器 - 完全忽略
@Component
public class IgnoreDiffRuleExecutor implements DiffRuleExecutor {
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        return false;
    }
}

// TOLERANCE执行器 - 数值容差
@Component
public class ToleranceDiffRuleExecutor implements DiffRuleExecutor {
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        if (item.getDiffType() != DiffType.MODIFY) {
            return true;
        }
        try {
            double oldVal = Double.parseDouble(item.getOldValue());
            double newVal = Double.parseDouble(item.getNewValue());
            double tolerance = Double.parseDouble(rule.getRuleValue());
            return Math.abs(oldVal - newVal) > tolerance;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

// SCRIPT执行器 - SpEL表达式
@Component
public class ScriptDiffRuleExecutor implements DiffRuleExecutor {
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("oldValue", item.getOldValue());
            context.setVariable("newValue", item.getNewValue());
            context.setVariable("fieldPath", item.getFieldPath());
            context.setVariable("diffType", item.getDiffType().name());

            Expression expression = parser.parseExpression(rule.getRuleValue());
            Object result = expression.getValue(context);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Script execute failed: {}", rule.getRuleValue(), e);
            return true;
        }
    }
}

// 规则执行器注册表
@Component
// SORT规则说明：
// fieldPath 指向待排序数组路径（如 data.orders），ruleValue 指定排序字段（如 id），
// 在生成JSON Patch前会先对old/new两侧数组按该字段排序。
public class DiffRuleExecutorRegistry {

    private final Map<String, DiffRuleExecutor> executors = new HashMap<>();

    public DiffRuleExecutorRegistry(
            Map<String, DiffRuleExecutor> executorMap) {
        executorMap.forEach((name, executor) -> {
            executors.put(name.toLowerCase().replace("diffruleexecutor", ""), executor);
        });
    }

    public DiffRuleExecutor getExecutor(String ruleType) {
        return executors.get(ruleType.toLowerCase());
    }
}
```
                        .newValue(newNode.asText())
                        .diffType(DiffType.MODIFY)
                        .build());
            }
            return diffItems;
        }

        // Array类型
        if (oldNode.isArray() && newNode.isArray()) {
            return compareArray(path, oldNode, newNode);
        }

        // Object类型
        if (oldNode.isObject() && newNode.isObject()) {
            return compareObject(path, oldNode, newNode);
        }

        // 类型不同
        diffItems.add(DiffItem.builder()
                .fieldPath(path)
                .oldValue(oldNode.toString())
                .newValue(newNode.toString())
                .diffType(DiffType.MODIFY)
                .build());

        return diffItems;
    }

    /**
     * 比对Object
     */
    private List<DiffItem> compareObject(String path, JsonNode oldNode, JsonNode newNode) {
        List<DiffItem> diffItems = new ArrayList<>();
        Iterator<String> oldFields = oldNode.fieldNames();
        Iterator<String> newFields = newNode.fieldNames();

        Set<String> allFields = new HashSet<>();
        oldNode.fieldNames().forEachRemaining(allFields::add);
        newNode.fieldNames().forEachRemaining(allFields::add);

        for (String field : allFields) {
            String fieldPath = path.isEmpty() ? field : path + "." + field;
            JsonNode oldVal = oldNode.get(field);
            JsonNode newVal = newNode.get(field);

            if (oldVal == null) {
                diffItems.add(DiffItem.builder()
                        .fieldPath(fieldPath)
                        .oldValue(null)
                        .newValue(newVal != null ? newVal.toString() : null)
                        .diffType(DiffType.ADD)
                        .build());
            } else if (newVal == null) {
                diffItems.add(DiffItem.builder()
                        .fieldPath(fieldPath)
                        .oldValue(oldVal.toString())
                        .newValue(null)
                        .diffType(DiffType.REMOVE)
                        .build());
            } else {
                diffItems.addAll(compare(fieldPath, oldVal, newVal));
            }
        }

        return diffItems;
    }

    /**
     * 比对Array
     */
    private List<DiffItem> compareArray(String path, JsonNode oldNode, JsonNode newNode) {
        List<DiffItem> diffItems = new ArrayList<>();
        int maxLen = Math.max(oldNode.size(), newNode.size());

        for (int i = 0; i < maxLen; i++) {
            String elementPath = path + "[" + i + "]";
            JsonNode oldVal = i < oldNode.size() ? oldNode.get(i) : null;
            JsonNode newVal = i < newNode.size() ? newNode.get(i) : null;

            if (oldVal == null) {
                diffItems.add(DiffItem.builder()
                        .fieldPath(elementPath)
                        .oldValue(null)
                        .newValue(newVal.toString())
                        .diffType(DiffType.ADD)
                        .build());
            } else if (newVal == null) {
                diffItems.add(DiffItem.builder()
                        .fieldPath(elementPath)
                        .oldValue(oldVal.toString())
                        .newValue(null)
                        .diffType(DiffType.REMOVE)
                        .build());
            } else {
                diffItems.addAll(compare(elementPath, oldVal, newVal));
            }
        }

        return diffItems;
    }
}
```

#### 5.4.5 规则执行器设计（补充示例）

```java
// 规则执行器接口
public interface DiffRuleExecutor {
    /**
     * 是否需要报告差异
     */
    boolean shouldReport(DiffItem item, DiffRule rule);
}

// IGNORE执行器 - 完全忽略
@Component
public class IgnoreDiffRuleExecutor implements DiffRuleExecutor {
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        return false;
    }
}

// TOLERANCE执行器 - 数值容差
@Component
public class ToleranceDiffRuleExecutor implements DiffRuleExecutor {
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        if (item.getDiffType() != DiffType.MODIFY) {
            return true;
        }
        try {
            double oldVal = Double.parseDouble(item.getOldValue());
            double newVal = Double.parseDouble(item.getNewValue());
            double tolerance = Double.parseDouble(rule.getRuleValue());
            return Math.abs(oldVal - newVal) > tolerance;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

// SCRIPT执行器 - SpEL表达式
@Component
public class ScriptDiffRuleExecutor implements DiffRuleExecutor {
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("oldValue", item.getOldValue());
            context.setVariable("newValue", item.getNewValue());
            context.setVariable("fieldPath", item.getFieldPath());
            context.setVariable("diffType", item.getDiffType().name());

            Expression expression = parser.parseExpression(rule.getRuleValue());
            Object result = expression.getValue(context);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Script execute failed: {}", rule.getRuleValue(), e);
            return true;
        }
    }
}

// 规则执行器注册表
@Component
// SORT规则说明：
// fieldPath 指向待排序数组路径（如 data.orders），ruleValue 指定排序字段（如 id），
// 在生成JSON Patch前会先对old/new两侧数组按该字段排序。
public class DiffRuleExecutorRegistry {

    private final Map<String, DiffRuleExecutor> executors = new HashMap<>();

    public DiffRuleExecutorRegistry(
            Map<String, DiffRuleExecutor> executorMap) {
        // 将所有DiffRuleExecutor Bean注册进来
        executorMap.forEach((name, executor) -> {
            executors.put(name.toLowerCase().replace("diffruleexecutor", ""), executor);
        });
    }

    public DiffRuleExecutor getExecutor(String ruleType) {
        return executors.get(ruleType.toLowerCase());
    }
}
```

#### 5.4.6 配置中心拉取

```java
@Service
public class ConfigCenterService {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    /**
     * 拉取Diff规则
     */
    public List<DiffRule> getDiffRules(String migrationKey) {
        try {
            String dataId = "migration_" + migrationKey;
            String config = nacosConfigManager.getConfig(dataId, "DEFAULT_GROUP", 5000);
            if (StringUtils.isEmpty(config)) {
                return Collections.emptyList();
            }
            return JSON.parseArray(config, DiffRule.class);
        } catch (Exception e) {
            log.warn("Failed to get diff rules for {}", migrationKey, e);
            return Collections.emptyList();
        }
    }
}
```

### 5.5 API接口设计

```java
/**
 * Diff服务API
 * 注意：此API仅供内部SDK调用，建议配置内部网络访问
 */
@RestController
@RequestMapping("/api/v1/diff")
public class DiffController {

    @Autowired
    private DiffService diffService;

    /**
 * 执行Diff比对
 * POST /api/v1/diff
 */
    @PostMapping
    public Result<DiffResult> execute(@RequestBody DiffRequest request) {
        DiffResult result = diffService.execute(request);
        return Result.success(result);
    }
}
```

### 5.6 模块依赖

```
migration-diff
├── spring-boot-starter-web
├── jackson-databind
├── mybatis-plus
├── mysql-connector-java
├── nacos-client
├── com.flipkart:zjsonpatch:0.4.16    # JSON Patch生成
└── com.jayway.jsonpath:json-path  # JSON Path查询
```

### 5.7 DDD分层架构（强制）

`migration-diff` 按 DDD 四层组织，聚焦“Diff比对”领域：

```
top.bulgat.migration.diff
├── interfaces/                  # 接口层（DiffController、请求响应DTO）
│   ├── rest/
│   ├── dto/
│   └── assembler/
├── application/                 # 应用层（Diff用例编排）
│   ├── service/
│   ├── command/
│   └── query/
├── domain/                      # 领域层（规则模型、比对引擎、领域服务）
│   ├── model/
│   ├── service/
│   ├── repository/
│   └── rule/
└── infrastructure/              # 基础设施层（规则拉取、记录落库、脚本执行器）
    ├── configcenter/
    ├── persistence/
    ├── repository/
    └── script/
```

约束说明：
- Diff核心算法、规则匹配、规则执行器注册属于领域层。
- 应用层只负责编排：接收请求 → 调用领域服务 → 持久化记录。
- 配置中心规则读取与数据库写入通过 infrastructure 实现 domain 仓储接口。
- Controller 不直接访问 Mapper/DAO，统一经 application service 进入领域模型。

---




### 参数校验补充（接口层）

`POST /api/v1/diff` 请求参数约束：
- `old_cost_time_ms`、`new_cost_time_ms` 为可选字段；传值时必须 `>= 0`。
- 应用层也会做参数兜底校验：`migration_key`、`old_json`、`new_json` 必填；`old_cost_time_ms/new_cost_time_ms` 若传入必须 `>= 0`。
- `migration_key` 限制：非空、长度不超过 128、且不能包含空格（应用层兜底校验）。
- 当规则路径为 `$.items[*].*` 这类“数组通配 + 对象通配”时，会正确匹配数组元素下所有子字段。
- `$.items[*].name` matches only the `name` field under each array element, and will not match sibling fields like `nickname`.
- Path matching accepts both `$`-prefixed and non-`$` paths; rules are normalized internally before matching.
- When loading rules from Nacos fails (timeout/network/config parse error), migration-diff falls back to an empty rule list to avoid blocking diff execution.
- Invalid rule items (for example unknown rule_type) are skipped individually during rule loading; valid items in the same config still take effect.
- Nacos rule payload `null` is treated as empty rule list, and `null` items inside the rule array are ignored.
- Fallback logs keep concise WARN messages with explicit `reason`, while full exception stack is moved to DEBUG for troubleshooting.
- SORT rules support nested array paths with wildcard (e.g. `$.groups[*].items`) and also support JsonPath sort keys (e.g. `$.user.id`).
- Nacos fallback WARN logs normalize whitespace in error reasons (single-line), while full stack traces remain in DEBUG for deep troubleshooting.
- Interface-layer bean validation now enforces `migration_key` max length `128` and no-whitespace pattern; invalid requests are rejected before entering application service.
