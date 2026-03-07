package top.bulgat.migration.sdk.core.support;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 默认降级实现，委托旧方法执行。
 *
 * @param <T> return type
 */
public final class DefaultOldFallback<T> implements BiFunction<Object[], Exception, T> {

    private final Function<Object[], T> oldMethod;

    /**
     * 创建默认旧方法降级器。
     *
     * @param oldMethod 旧方法
     */
    public DefaultOldFallback(Function<Object[], T> oldMethod) {
        this.oldMethod = oldMethod;
    }

    /**
     * 执行降级逻辑。
     *
     * @param args 原始调用参数
     * @param ex 策略执行期间抛出的异常
     * @return 降级结果
     */
    @Override
    public T apply(Object[] args, Exception ex) {
        if (ex instanceof OldInvocationFailedException marker) {
            Exception oldError = marker.getOldError();
            if (oldError instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(oldError);
        }
        return oldMethod.apply(args);
    }
}
