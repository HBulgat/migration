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
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;
import top.bulgat.migration.sdk.core.support.DefaultOldFallback;

/**
 * Execution context for migration strategies.
 *
 * @param <T> return type
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
    private final Object[] args;
    private final DiffServiceCaller diffServiceCaller;
    private final GrayscaleMatcher grayscaleMatcher;
    private final ExecutorService executorService;

    /**
     * Builds grayscale params through ParamHandler.
     *
     * @return grayscale params
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
     * Indicates whether fallback uses SDK default old-method fallback.
     *
     * @return true when fallback is default old-method fallback
     */
    public boolean isDefaultOldFallback() {
        return fallbackMethod instanceof DefaultOldFallback<?>;
    }
}
