# migration-sdk-core 模块设计


### 2.1 模块职责

提供Java SDK核心能力，负责：
- 从配置中心拉取迁移配置和灰度规则
- 根据迁移状态决定调用策略（旧接口/新接口/并发）
- **异步**调用Diff服务进行比对
- 返回最终结果

**核心设计理念**：迁移的可以是HTTP接口、Java方法、Go函数、RPC接口等，SDK只负责调用和决策，不限制具体形式。

### 2.2 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 基础语言 |
| Lombok | 1.18.30 | 代码简化 |
| FastJSON2 | 2.0.52 | JSON序列化 |
| Apache HttpClient | 4.5.14 | HTTP调用 |
| Cglib | 3.3.0 | 动态代理 |
| commons-lang3 | 3.14.0 | 工具类 |
| Disruptor | 3.4.4 | 异步消息队列 |

### 2.3 核心类设计

```java
// 迁移配置类
// 注意：不再包含URL字段，因为迁移目标可以是方法、函数等，不一定是HTTP接口
@Data
@Builder
public class MigrationConfig {
    private String migrationKey;      // 迁移任务唯一标识
    private Integer status;          // 当前迁移状态(1-7)
    private String description;      // 描述
    private Integer timeout;         // 超时时间(ms)
}

// 灰度规则配置
@Data
@Builder
public class GrayConfig {
    private String migrationKey;      // 迁移任务key
    private String ruleType;         // PERCENTAGE/BLACKLIST/WHITELIST/EXPRESSION
    private String ruleValue;        // 规则值
    private Boolean enable;          // 是否启用
}

// Diff规则配置 (存储在配置中心，供Diff服务使用)
@Data
@Builder
public class DiffConfig {
    private String migrationKey;      // 迁移任务key
    private String ruleType;         // IGNORE/TOLERANCE/SCRIPT/SORT
    private String fieldPath;        // 字段路径
    private String ruleValue;        // 规则值
    private Boolean enable;          // 是否启用
}

// 迁移状态枚举
public enum MigrationTaskStatus {
    OLD(1, "单旧"),
    VALIDATION_GRAY(2, "验证-灰度"),
    VALIDATION_ALL(3, "验证-全开"),
    GO_LIVE_GRAY(4, "上线-灰度"),
    GO_LIVE_ALL(5, "上线-全开"),
    DECOMMISSIONING_GRAY(6, "停用-灰度"),
    DECOMMISSIONING_ALL(7, "停用-全开");

    private final int code;
    private final String desc;

    public static MigrationTaskStatus fromCode(int code) {
        for (MigrationTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}

// 灰度规则类型枚举
public enum GrayRuleType {
    PERCENTAGE,      // 百分比
    BLACKLIST,       // 黑名单
    WHITELIST,       // 白名单
    EXPRESSION       // 表达式
}

// Diff规则类型枚举
public enum DiffRuleType {
    IGNORE,          // 忽略
    TOLERANCE,       // 容差
    SCRIPT,          // SpEL脚本
    SORT             // 排序预处理
}

// Diff差异类型枚举
public enum DiffType {
    MODIFY,          // 修改
    ADD,             // 新增
    REMOVE           // 删除
}
```

### 2.4 核心接口设计

```java
// 参数处理器接口 - 将方法参数转换为灰度匹配参数
@FunctionalInterface
public interface ParamHandler {
    /**
     * 将原接口参数转换为灰度匹配参数
     * @param args 原接口的方法参数
     * @return 灰度匹配参数Map
     */
    Map<String, Object> build(Object... args);
}

// 执行函数接口 - 封装后的可执行函数
@FunctionalInterface
public interface ExecuteFunction<T> {
    /**
     * 执行迁移逻辑
     * @param args 方法参数
     * @return 执行结果
     */
    T apply(Object... args);
}

// 配置客户端接口 - 从配置中心拉取配置（只读，不做写入）
public interface ConfigClient {
    /**
     * 拉取迁移配置
     * @param migrationKey 迁移任务key
     * @return 迁移配置
     */
    MigrationConfig getMigrationConfig(String migrationKey);

    /**
     * 拉取灰度规则
     * @param migrationKey 迁移任务key
     * @return 灰度规则列表
     */
    List<GrayConfig> getGrayRules(String migrationKey);
}

// Diff服务调用器接口 - 异步调用Diff服务
public interface DiffServiceCaller {
    /**
     * 异步执行Diff比对
     * @param request Diff请求
     */
    void executeDiffAsync(DiffRequest request);
}
```

### 2.5 策略模式设计

```java
// 调用策略接口
public interface MigrationStrategy<T> {
    /**
     * 执行迁移调用
     * @param oldMethod 旧方法
     * @param newMethod 新方法
     * @param fallbackMethod 降级方法
     * @param paramHandler 参数处理器
     * @param args 方法参数
     * @return 执行结果
     */
    T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            Object[] args
    );
}

// 策略注册表
@Component
public class MigrationStrategyRegistry {

    private final Map<MigrationTaskStatus, MigrationStrategy<?>> strategies = new HashMap<>();

    @Autowired
    public MigrationStrategyRegistry(
            List<MigrationStrategy> strategyList) {
        for (MigrationStrategy strategy : strategyList) {
            strategies.put(strategy.getStatus(), strategy);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> MigrationStrategy<T> getStrategy(MigrationTaskStatus status) {
        return (MigrationStrategy<T>) strategies.get(status);
    }
}

// OLD策略 - 只调用旧接口
@Component
public class OldOnlyStrategy<T> implements MigrationStrategy<T> {

    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.OLD;
    }

    @Override
    public T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            Object[] args) {
        try {
            return oldMethod.apply(args);
        } catch (Exception e) {
            return fallbackMethod.apply(args, e);
        }
    }
}

// VALIDATION_GRAY/VALIDATION_ALL策略
@Component
public class ValidationStrategy<T> implements MigrationStrategy<T> {

    @Autowired
    private ConfigClient configClient;

    @Autowired
    private DiffServiceCaller diffServiceCaller;

    private final MigrationTaskStatus targetStatus;

    public ValidationStrategy(MigrationTaskStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    @Override
    public MigrationTaskStatus getStatus() {
        return targetStatus;
    }

    @Override
    public T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            Object[] args) {
        // 并发调用新旧接口
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<T> oldFuture = executor.submit(() -> oldMethod.apply(args));
            Future<T> newFuture = executor.submit(() -> newMethod.apply(args));

            T oldResult = oldFuture.get();
            T newResult = newFuture.get();

            // 异步发送Diff请求
            diffServiceCaller.executeDiffAsync(DiffRequest.builder()
                    .migrationKey(getMigrationKey())
                    .oldJson(serialize(oldResult))
                    .newJson(serialize(newResult))
                    .build());

            // 始终返回旧接口结果
            return oldResult;
        } catch (Exception e) {
            return fallbackMethod.apply(args, e);
        } finally {
            executor.shutdown();
        }
    }
}

// GO_LIVE_GRAY策略
@Component
public class GoLiveGrayStrategy<T> implements MigrationStrategy<T> {

    @Autowired
    private ConfigClient configClient;

    @Autowired
    private DiffServiceCaller diffServiceCaller;

    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.GO_LIVE_GRAY;
    }

    @Override
    public T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            Object[] args) {
        // 获取灰度参数
        Map<String, Object> grayParam = paramHandler.build(args);

        // 判断是否命中灰度
        boolean hitGray = matchGray(grayParam);

        if (hitGray) {
            // 命中灰度，调用新接口
            return newMethod.apply(args);
        } else {
            // 未命中灰度，并发调用做Diff
            // ... 类似ValidationStrategy
            return oldMethod.apply(args);
        }
    }
}

// GO_LIVE_ALL策略
@Component
public class GoLiveAllStrategy<T> implements MigrationStrategy<T> {
    // 类似ValidationStrategy，但返回新接口结果
}

// DECOMMISSIONING_GRAY策略
@Component
public class DecommissioningGrayStrategy<T> implements MigrationStrategy<T> {
    // 命中灰度：只调用新接口
    // 未命中灰度：并发调用，Diff，返回新接口结果
}

// DECOMMISSIONING_ALL策略
@Component
public class DecommissioningAllStrategy<T> implements MigrationStrategy<T> {
    // 类似OldOnlyStrategy，但调用新接口
}
```

### 2.6 迁移客户端（使用策略模式）

```java
public class MigrationClient {

    private final MigrationConfig config;
    private final ConfigClient configClient;
    private final DiffServiceCaller diffServiceCaller;
    private final MigrationStrategyRegistry strategyRegistry;

    /**
     * 包装方法，将旧接口、新接口、降级方法包装成可执行的函数
     */
    public <T> ExecuteFunction<T> wrap(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler) {

        return (args) -> {
            // 从配置中心获取最新配置
            MigrationConfig migrationConfig = configClient.getMigrationConfig(config.getMigrationKey());
            int status = migrationConfig.getStatus();
            MigrationTaskStatus MigrationTaskStatus = MigrationTaskStatus.fromCode(status);

            // 获取对应策略并执行
            MigrationStrategy<T> strategy = strategyRegistry.getStrategy(MigrationTaskStatus);
            return strategy.execute(oldMethod, newMethod, fallbackMethod, paramHandler, args);
        };
    }
}
```

### 2.7 异步Diff调用设计

使用Disruptor实现高性能异步消息队列：

```java
@Component
public class DiffServiceCallerImpl implements DiffServiceCaller {

    private final RingBuffer<DiffEvent> ringBuffer;

    public DiffServiceCallerImpl() {
        Disruptor<DiffEvent> disruptor = new Disruptor<>(
                DiffEvent::new,
                1024 * 1024,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            // 实际发送HTTP请求到Diff服务
            sendToDiffService(event.getRequest());
        });

        disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
    }

    @Override
    public void executeDiffAsync(DiffRequest request) {
        long sequence = ringBuffer.next();
        try {
            DiffEvent event = ringBuffer.get(sequence);
            event.setRequest(request);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
```

### 2.8 模块依赖

```
migration-sdk-core
├── fastjson2
├── apache-httpclient
├── cglib
├── commons-lang3
└── disruptor
```

---
