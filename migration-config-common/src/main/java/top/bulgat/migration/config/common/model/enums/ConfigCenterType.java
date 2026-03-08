package top.bulgat.migration.config.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ConfigCenterType {
    MEMORY(true),
    NACOS(true),
    REDIS(false),
    ETCD(false),
    ZOOKEEPER(false),
    CONSUL(false);
    @Getter
    private final boolean supported;
}
