package top.bulgat.migration.sdk.core.support;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default fallback implementation that delegates to old method.
 *
 * @param <T> return type
 */
public final class DefaultOldFallback<T> implements BiFunction<Object[], Exception, T> {

    private final Function<Object[], T> oldMethod;

    /**
     * Creates default old-method fallback.
     *
     * @param oldMethod old method
     */
    public DefaultOldFallback(Function<Object[], T> oldMethod) {
        this.oldMethod = oldMethod;
    }

    /**
     * Applies fallback logic.
     *
     * @param args original invocation args
     * @param ex failure raised in strategy execution
     * @return fallback result
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
