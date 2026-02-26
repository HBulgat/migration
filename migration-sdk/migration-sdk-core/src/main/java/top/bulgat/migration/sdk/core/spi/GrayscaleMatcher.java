package top.bulgat.migration.sdk.core.spi;

import java.util.List;
import java.util.Map;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;

/**
 * 灰度匹配器接口。
 */
public interface GrayscaleMatcher {

    /**
     * 判断当前请求是否命中灰度规则。
     *
     * @param rules 灰度规则
     * @param params 参数处理器构造的灰度参数
     * @return true 表示命中灰度
     */
    boolean match(List<GrayscaleConfig> rules, Map<String, Object> params);
}
