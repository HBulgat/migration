package top.bulgat.migration.config.common.config;

import lombok.Data;
import top.bulgat.migration.config.common.model.enums.ConfigCenterType;

import java.util.Map;

@Data
public class ConfigCenterProperties {
    private ConfigCenterType type;
    private String serverAddr;
    private String username;
    private String password;
    private Map<String,String> metaInfo;
}
