package top.bulgat.migration.sdk.core.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import top.bulgat.migration.sdk.core.function.ParamHandler;
import top.bulgat.migration.sdk.core.extension.DiffPostProcessor;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;
import top.bulgat.migration.sdk.core.support.DefaultOldFallback;

/**
 * 迁移策略的执行上下文。
 *
 * @param <T> 返回值类型
 */
@Getter
@Builder
public class MigrationExecutionContext<T> {

    private final String migrationKey;
    private final List<GrayscaleConfig> grayscaleRules;
    private final Function<Object[], T> oldMethod;
    private final Function<Object[], T> newMethod;
    private final BiFunction<Object[], Exception, T> fallbackMethod;
    private final ParamHandler paramHandler;
    private final DiffPostProcessor postProcessor;
    private final Object[] args;
    private final DiffServiceCaller diffServiceCaller;
    private final GrayscaleMatcher grayscaleMatcher;
    private final ExecutorService executorService;
    private final int migrationTaskStatus;

    /**
     * 通过 ParamHandler 构建灰度匹配参数。
     *
     * @return 灰度参数映射表
     */
    public Map<String, Object> buildParam() {
        if (paramHandler == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> params = paramHandler.build(args);
        if (params == null) {
            return Collections.emptyMap();
        }
        return params;
    }

    /**
     * 判断当前的降级逻辑是否为SDK默认的“调用旧接口”降级。
     *
     * @return 如果为默认降级，返回 true
     */
    public boolean isDefaultOldFallback() {
        return fallbackMethod instanceof DefaultOldFallback<?>;
    }
}
