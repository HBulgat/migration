package top.bulgat.migration.sdk.core.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;

/**
 * 迁移策略注册表：维护状态到策略实现的映射。
 */
public class MigrationStrategyRegistry {

    private final Map<MigrationTaskStatus, MigrationStrategy> strategies = new EnumMap<>(MigrationTaskStatus.class);

    /**
     * 构造策略注册表。
     *
     * @param strategyList 策略列表
     */
    public MigrationStrategyRegistry(List<MigrationStrategy> strategyList) {
        for (MigrationStrategy strategy : strategyList) {
            strategies.put(strategy.getStatus(), strategy);
        }
    }

    /**
     * 构建默认策略注册表（包含 7 个内置迁移状态策略）。
     *
     * @return 策略注册表
     */
    public static MigrationStrategyRegistry defaultRegistry() {
        return new MigrationStrategyRegistry(List.of(
                new OldOnlyStrategy(),
                new ValidationGrayStrategy(),
                new ValidationAllStrategy(),
                new GoLiveGrayStrategy(),
                new GoLiveAllStrategy(),
                new DecommissioningGrayStrategy(),
                new DecommissioningAllStrategy()));
    }

    /**
     * 获取指定状态对应的策略。
     *
     * @param status 迁移状态
     * @return 策略；不存在时返回 null
     */
    public MigrationStrategy getStrategy(MigrationTaskStatus status) {
        return strategies.get(status);
    }
}
