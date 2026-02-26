package top.bulgat.migration.sdk.core.function;

import java.util.Map;

/**
 * 参数处理器：将业务方法参数转换为灰度匹配参数。
 */
@FunctionalInterface
public interface ParamHandler {

    /**
     * 构建灰度匹配参数。
     *
     * @param args 原始方法参数
     * @return 灰度匹配参数
     */
    Map<String, Object> build(Object... args);
}
