package top.bulgat.migration.diff.domain.model;
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
