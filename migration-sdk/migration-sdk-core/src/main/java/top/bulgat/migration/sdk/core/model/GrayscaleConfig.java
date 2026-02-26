package top.bulgat.migration.sdk.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灰度规则配置模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrayscaleConfig {

    private String ruleId;
    private String migrationKey;
    private String ruleType;
    private String ruleValue;
    private Boolean enable;
}
