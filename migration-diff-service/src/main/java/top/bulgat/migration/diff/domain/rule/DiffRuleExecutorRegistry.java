package top.bulgat.migration.diff.domain.rule;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.domain.model.DiffRuleType;

/**
 * Diff规则执行器注册表。
 * 根据规则类型路由到对应的执行器实现。
 */
@Component
public class DiffRuleExecutorRegistry {

    private final Map<DiffRuleType, DiffRuleExecutor> executors = new EnumMap<>(DiffRuleType.class);

    public DiffRuleExecutorRegistry(List<DiffRuleExecutor> ruleExecutors) {
        for (DiffRuleExecutor executor : ruleExecutors) {
            executors.put(executor.supports(), executor);
        }
    }

    /**
     * 获取指定规则类型对应的执行器。
     *
     * @param type 规则类型
     * @return 执行器，若未注册则返回null
     */
    public DiffRuleExecutor getExecutor(DiffRuleType type) {
        return executors.get(type);
    }
}

