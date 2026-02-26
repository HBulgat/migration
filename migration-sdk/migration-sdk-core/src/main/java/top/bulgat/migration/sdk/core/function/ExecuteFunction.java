package top.bulgat.migration.sdk.core.function;

/**
 * 可执行迁移函数，封装经过 SDK 路由后的调用动作。
 *
 * @param <T> 返回值类型
 */
@FunctionalInterface
public interface ExecuteFunction<T> {

    /**
     * 执行迁移逻辑。
     *
     * @param args 原始方法参数
     * @return 最终返回结果
     */
    T apply(Object... args);
}
