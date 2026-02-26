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
import top.bulgat.migration.sdk.core.config.HttpConfigClient;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;
import top.bulgat.migration.sdk.core.diff.DisruptorDiffServiceCaller;
import top.bulgat.migration.sdk.core.function.ExecuteFunction;
import top.bulgat.migration.sdk.core.function.ParamHandler;
import top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
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
     * Creates client with default HTTP implementations.
     *
     * @param config base migration config
     */
    public MigrationClient(MigrationConfig config) {
        this(config,
                MigrationSdkProperties.fromEnv(),
                Executors.newFixedThreadPool(2));
    }

    /**
     * Creates client with default SPI implementations and external executor.
     *
     * @param config base migration config
     * @param properties sdk properties
     * @param executorService executor for async branch invocation
     */
    public MigrationClient(MigrationConfig config, MigrationSdkProperties properties, ExecutorService executorService) {
        this(
                config,
                new HttpConfigClient(properties),
                new DisruptorDiffServiceCaller(properties),
                new DefaultGrayscaleMatcher(),
                MigrationStrategyRegistry.defaultRegistry(),
                executorService,
                true);
    }

    /**
     * Creates client with custom dependencies.
     *
     * @param config base migration config
     * @param configClient config client
     * @param diffServiceCaller diff caller
     * @param grayscaleMatcher grayscale matcher
     * @param strategyRegistry strategy registry
     * @param executorService executor for async branch invocation
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
     * Internal constructor.
     *
     * @param config base migration config
     * @param configClient config client
     * @param diffServiceCaller diff caller
     * @param grayscaleMatcher grayscale matcher
     * @param strategyRegistry strategy registry
     * @param executorService executor service
     * @param manageResources whether this instance owns resources lifecycle
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
     * Wraps old/new methods and defaults fallback to old method.
     *
     * @param oldMethod old branch
     * @param newMethod new branch
     * @param paramHandler grayscale param handler
     * @param <T> return type
     * @return executable migration function
     */
    public <T> ExecuteFunction<T> wrap(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            ParamHandler paramHandler) {
        return wrap(oldMethod, newMethod, null, paramHandler);
    }

    /**
     * Wraps old/new methods with explicit fallback.
     *
     * @param oldMethod old branch
     * @param newMethod new branch
     * @param fallbackMethod fallback branch
     * @param paramHandler grayscale param handler
     * @param <T> return type
     * @return executable migration function
     */
    public <T> ExecuteFunction<T> wrap(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler) {
        Objects.requireNonNull(oldMethod, "oldMethod is required");
        Objects.requireNonNull(newMethod, "newMethod is required");

        BiFunction<Object[], Exception, T> safeFallback = fallbackMethod == null
                ? new DefaultOldFallback<>(oldMethod)
                : fallbackMethod;

        return args -> execute(oldMethod, newMethod, safeFallback, paramHandler, args);
    }

    /**
     * Executes one routed invocation.
     *
     * @param oldMethod old branch
     * @param newMethod new branch
     * @param fallbackMethod fallback branch
     * @param paramHandler grayscale parameter handler
     * @param args invocation arguments
     * @param <T> return type
     * @return routed invocation result
     */
    private <T> T execute(
            Function<Object[], T> oldMethod,
            Function<Object[], T> newMethod,
            BiFunction<Object[], Exception, T> fallbackMethod,
            ParamHandler paramHandler,
            Object[] args) {
        try {
            MigrationConfig latestConfig = loadLatestConfig();
            MigrationStatus status = resolveStatus(latestConfig.getStatus());
            List<GrayscaleConfig> grayscaleRules = loadGrayscaleRules();

            MigrationStrategy strategy = strategyRegistry.getStrategy(status);
            if (strategy == null) {
                log.warn("strategy not found, fallback to OLD, migrationKey={}, status={}",
                        config.getMigrationKey(),
                        status);
                strategy = strategyRegistry.getStrategy(MigrationStatus.OLD);
            }

            MigrationExecutionContext<T> context = MigrationExecutionContext.<T>builder()
                    .migrationKey(config.getMigrationKey())
                    .grayscaleRules(grayscaleRules)
                    .oldMethod(oldMethod)
                    .newMethod(newMethod)
                    .fallbackMethod(fallbackMethod)
                    .paramHandler(paramHandler)
                    .args(args)
                    .diffServiceCaller(diffServiceCaller)
                    .grayscaleMatcher(grayscaleMatcher)
                    .executorService(executorService)
                    .build();
            return strategy.execute(context);
        } catch (Exception ex) {
            return fallbackMethod.apply(args, ex);
        }
    }

    /**
     * Loads latest migration config and falls back to OLD when unavailable.
     *
     * @return latest migration config
     */
    private MigrationConfig loadLatestConfig() {
        try {
            MigrationConfig latestConfig = configClient.getMigrationConfig(config.getMigrationKey());
            if (latestConfig == null) {
                return MigrationConfig.builder()
                        .migrationKey(config.getMigrationKey())
                        .status(MigrationStatus.OLD.getCode())
                        .build();
            }
            if (latestConfig.getMigrationKey() == null || latestConfig.getMigrationKey().isBlank()) {
                latestConfig.setMigrationKey(config.getMigrationKey());
            }
            if (latestConfig.getStatus() == null) {
                latestConfig.setStatus(MigrationStatus.OLD.getCode());
            }
            return latestConfig;
        } catch (Exception ex) {
            log.warn("load migration config failed, fallback to OLD, migrationKey={}", config.getMigrationKey(), ex);
            return MigrationConfig.builder()
                    .migrationKey(config.getMigrationKey())
                    .status(MigrationStatus.OLD.getCode())
                    .build();
        }
    }

    /**
     * Loads grayscale rules and returns empty list when unavailable.
     *
     * @return grayscale rules
     */
    private List<GrayscaleConfig> loadGrayscaleRules() {
        try {
            List<GrayscaleConfig> rules = configClient.getGrayscaleRules(config.getMigrationKey());
            return rules == null ? Collections.emptyList() : rules;
        } catch (Exception ex) {
            log.warn("load grayscale rules failed, migrationKey={}", config.getMigrationKey(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Resolves status code to {@link MigrationStatus}.
     *
     * @param statusCode status code
     * @return resolved status, or OLD for invalid input
     */
    private MigrationStatus resolveStatus(Integer statusCode) {
        if (statusCode == null) {
            return MigrationStatus.OLD;
        }
        try {
            return MigrationStatus.fromCode(statusCode);
        } catch (Exception ex) {
            log.warn("invalid migration status code, migrationKey={}, statusCode={}",
                    config.getMigrationKey(),
                    statusCode);
            return MigrationStatus.OLD;
        }
    }

    /**
     * Closes resources owned by this client.
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
