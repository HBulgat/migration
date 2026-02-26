package top.bulgat.migration.sdk.core.support;

/**
 * 单次调用结果封装，包含返回值、异常和耗时。
 *
 * @param value 返回值
 * @param error 异常信息，成功时为 null
 * @param costTimeMs 耗时（毫秒）
 * @param <T> 返回值类型
 */
public record InvocationResult<T>(T value, Exception error, long costTimeMs) {

    /**
     * 构建成功结果。
     *
     * @param value 返回值
     * @param costTimeMs 耗时（毫秒）
     * @param <T> 返回值类型
     * @return 调用结果
     */
    public static <T> InvocationResult<T> success(T value, long costTimeMs) {
        return new InvocationResult<>(value, null, costTimeMs);
    }

    /**
     * 构建失败结果。
     *
     * @param error 异常
     * @param costTimeMs 耗时（毫秒）
     * @param <T> 返回值类型
     * @return 调用结果
     */
    public static <T> InvocationResult<T> failure(Exception error, long costTimeMs) {
        return new InvocationResult<>(null, error, costTimeMs);
    }

    /**
     * 是否调用成功。
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return error == null;
    }
}
