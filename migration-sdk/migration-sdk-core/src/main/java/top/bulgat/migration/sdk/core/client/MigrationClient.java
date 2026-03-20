package top.bulgat.migration.sdk.core.client;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.sdk.core.config.CachedConfigClient;
import top.bulgat.migration.sdk.core.config.HttpConfigClient;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;
import top.bulgat.migration.sdk.core.diff.DisruptorDiffServiceCaller;
import top.bulgat.migration.sdk.core.function.ExecuteFunction;
import top.bulgat.migration.sdk.core.function.ParamHandler;
import top.bulgat.migration.sdk.core.extension.DiffPostProcessor;
import top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.spi.ConfigClient;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;
import top.bulgat.migration.sdk.core.strategy.MigrationExecutionContext;
import top.bulgat.migration.sdk.core.strategy.MigrationStrategy;
import top.bulgat.migration.sdk.core.strategy.MigrationStrategyRegistry;
import top.bulgat.migration.sdk.core.support.DefaultOldFallback;

/**
 * 迁移客户端：根据迁移状态在新旧逻辑之间进行路由。
 */
public class MigrationClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MigrationClient.class);

    private final MigrationConfig config;
    private final ConfigClient configClient;
    private final DiffServiceCaller diffServiceCaller;
    private final GrayscaleMatcher grayscaleMatcher;
    private final MigrationStrategyRegistry strategyRegistry;
    private final ExecutorService executorService;
    private final boolean manageResources;

    /**
     * 创建迁移客户端，使用默认的HTTP实现。
     *
     * @param config 基础迁移配置
     */
    public MigrationClient(MigrationConfig config) {
        this(config,
                MigrationSdkProperties.fromEnv(),
                Executors.newFixedThreadPool(2));
    }

    /**
     * 创建迁移客户端，使用默认的SPI实现，并提供外部线程池用于并发调用。
     *
     * @param config          基础迁移配置
     * @param properties      SDK配置属性
     * @param executorService 用于异步分支调用的线程池
     */
    public MigrationClient(MigrationConfig config, MigrationSdkProperties properties, ExecutorService executorService) {
        this(
                config,
                new CachedConfigClient(new HttpConfigClient(properties), properties.getConfigCenterCacheRefreshIntervalSeconds()),
                new DisruptorDiffServiceCaller(properties),
                new DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                executorService,
                true);
    }

    /**
     * 创建迁移客户端，允许自定义所有核心依赖。
     *
     * @param config            基础迁移配置
     * @param configClient      配置中心客户端
     * @param diffServiceCaller Diff 服务调用器
     * @param grayscaleMatcher  灰度规则匹配器
     * @param strategyRegistry  策略注册表
     * @param executorService   异步调用线程池
     */
    public MigrationClient(
            MigrationConfig config,
            ConfigClient configClient,
            DiffServiceCaller diffServiceCaller,
            GrayscaleMatcher grayscaleMatcher,
            MigrationStrategyRegistry strategyRegistry,
            ExecutorService executorService) {
        this(config, configClient, diffServiceCaller, grayscaleMatcher, strategyRegistry, executorService, false);
    }

    /**
     * 内部构造函数。
     *
     * @param config            基础迁移配置
     * @param configClient      配置中心客户端
     * @param diffServiceCaller Diff 调用器
     * @param grayscaleMatcher  灰度匹配器
     * @param strategyRegistry  迁移策略注册表
     * @param executorService   线程池
     * @param manageResources   是否由当前实例管理资源的生命周期（关闭时释放）
     */
    private MigrationClient(
            MigrationConfig config,
            ConfigClient configClient,
            DiffServiceCaller diffServiceCaller,
            GrayscaleMatcher grayscaleMatcher,
            MigrationStrategyRegistry strategyRegistry,
            ExecutorService executorService,
            boolean manageResources) {
        this.config = Objects.requireNonNull(config, "config is required");
        if (this.config.getMigrationKey() == null || this.config.getMigrationKey().isBlank()) {
            throw new IllegalArgumentException("migrationKey is required");
        }
        this.configClient = Objects.requireNonNull(configClient, "configClient is required");
        this.diffServiceCaller = Objects.requireNonNull(diffServiceCaller, "diffServiceCaller is required");
        this.grayscaleMatcher = Objects.requireNonNull(grayscaleMatcher, "grayscaleMatcher is required");
        this.strategyRegistry = Objects.requireNonNull(strategyRegistry, "strategyRegistry is required");
        this.executorService = Objects.requireNonNull(executorService, "executorService is required");
        this.manageResources = manageResources;
    }

    /**
     * 包装新旧接口方法，如果没有提供降级方法，则默认降级逻辑是抛异常时调用旧接口。
     *
     * @param oldMethod    旧接口方法引用
     * @param newMethod    新接口方法引用
     * @param paramHandler 灰度参数处理器，用于将方法参数转换为灰度匹配字典
     * @param postProcessor 后置数据处理器，用于对比前清洗数据
     * @param <T>          方法返回值类型
     * @return 封装后的可执行函数
     */
    public <T> ExecuteFunction<T> wrap(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            ParamHandler paramHandler,
            DiffPostProcessor postProcessor) {
        return wrap(oldMethod, newMethod, null, paramHandler, postProcessor);
    }

    /**
     * 包装新旧接口方法，并显示指定降级逻辑方法。
     *
     * @param oldMethod      旧接口方法引用
     * @param newMethod      新接口方法引用
     * @param fallbackMethod 降级方法引用，入参包含原参数和异常对象
     * @param paramHandler   灰度参数处理器
     * @param postProcessor  后置数据处理器
     * @param <T>            返回值类型
     * @return 封装后的可执行函数
     */
    public <T> ExecuteFunction<T> wrap(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            DiffPostProcessor postProcessor) {
        Objects.requireNonNull(oldMethod, "oldMethod is required");

        Objects.requireNonNull(newMethod, "newMethod is required");

        BiFunction<Object[], Exception, T> safeFallback = fallbackMethod == null
                ? new DefaultOldFallback<>(oldMethod)
                : fallbackMethod;

        return args -> execute(oldMethod, newMethod, safeFallback, paramHandler, postProcessor, args);
    }

    /**
     * 根据当前迁移配置和策略执行一次路由调用。
     *
     * @param oldMethod      旧接口分支
     * @param newMethod      新接口分支
     * @param fallbackMethod 降级分支
     * @param paramHandler   灰度参数处理器
     * @param postProcessor  后置数据处理器
     * @param args           原始调用参数
     * @param <T>            返回值类型
     * @return 实际执行分支的返回结果
     */
    private <T> T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            DiffPostProcessor postProcessor,
            Object[] args) {
        try {
            // 从配置中心拉取最新配置和灰度规则
            MigrationConfig latestConfig = loadLatestConfig();
            MigrationTaskStatus status = resolveStatus(latestConfig.getStatus());
            List<GrayscaleConfig> grayscaleRules = loadGrayscaleRules();

            // 根据状态获取对应的策略
            MigrationStrategy strategy = strategyRegistry.getStrategy(status);
            if (strategy == null) {
                log.warn("无法找到对应的迁移策略, 降级为 OLD, migrationKey={}, status={}",
                        config.getMigrationKey(),
                        status);
                strategy = strategyRegistry.getStrategy(MigrationTaskStatus.OLD);
            }

            // 构建执行上下文
            MigrationExecutionContext<T> context = MigrationExecutionContext.<T>builder()
                    .migrationKey(config.getMigrationKey())
                    .grayscaleRules(grayscaleRules)
                    .oldMethod(oldMethod)
                    .newMethod(newMethod)
                    .migrationTaskStatus(status.getCode())
                    .fallbackMethod(fallbackMethod)
                    .paramHandler(paramHandler)
                    .postProcessor(postProcessor)
                    .args(args)
                    .diffServiceCaller(diffServiceCaller)
                    .grayscaleMatcher(grayscaleMatcher)
                    .executorService(executorService)
                    .build();

            // 执行迁移策略
            return strategy.execute(context);
        } catch (Exception ex) {
            // 发生异常时，均走降级方法
            return fallbackMethod.apply(args, ex);
        }
    }

    /**
     * 拉取最新的迁移任务配置信息，如果获取失败则默认降级为 OLD 状态。
     *
     * @return 最新的迁移配置
     */
    private MigrationConfig loadLatestConfig() {
        try {
            MigrationConfig latestConfig = configClient.getMigrationConfig(config.getMigrationKey());
            if (latestConfig == null) {
                return MigrationConfig.builder()
                        .migrationKey(config.getMigrationKey())
                        .status(MigrationTaskStatus.OLD.getCode())
                        .build();
            }
            if (latestConfig.getMigrationKey() == null || latestConfig.getMigrationKey().isBlank()) {
                latestConfig.setMigrationKey(config.getMigrationKey());
            }
            if (latestConfig.getStatus() == null) {
                latestConfig.setStatus(MigrationTaskStatus.OLD.getCode());
            }
            return latestConfig;
        } catch (Exception ex) {
            log.warn("load 迁移配置 failed, fallback to OLD, migrationKey={}", config.getMigrationKey(), ex);
            return MigrationConfig.builder()
                    .migrationKey(config.getMigrationKey())
                    .status(MigrationTaskStatus.OLD.getCode())
                    .build();
        }
    }

    /**
     * 拉取灰度规则，如果获取失败或没有规则，则返回空列表。
     *
     * @return 灰度规则列表
     */
    private List<GrayscaleConfig> loadGrayscaleRules() {
        try {
            List<GrayscaleConfig> rules = configClient.getGrayscaleRules(config.getMigrationKey());
            return rules == null ? Collections.emptyList() : rules;
        } catch (Exception ex) {
            log.warn("load 灰度规则 failed, migrationKey={}", config.getMigrationKey(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * 将配置的状态码解析为迁移状态枚举，解析失败时降级为 OLD。
     *
     * @param statusCode 状态码
     * @return 解析后的迁移状态
     */
    private MigrationTaskStatus resolveStatus(Integer statusCode) {
        if (statusCode == null) {
            return MigrationTaskStatus.OLD;
        }
        try {
            return MigrationTaskStatus.fromCode(statusCode);
        } catch (Exception ex) {
            log.warn("invalid migration status code, migrationKey={}, statusCode={}",
                    config.getMigrationKey(),
                    statusCode);
            return MigrationTaskStatus.OLD;
        }
    }

    /**
     * 关闭客户端，释放内部占用的连接和线程池资源。
     */
    @Override
    public void close() {
        if (!manageResources) {
            return;
        }
        try {
            configClient.close();
        } catch (Exception ex) {
            log.warn("close config client failed", ex);
        }
        try {
            diffServiceCaller.close();
        } catch (Exception ex) {
            log.warn("close diff service caller failed", ex);
        }
        executorService.shutdown();
    }
}
