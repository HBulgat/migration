package top.bulgat.migration.sdk.core.spi;

import java.util.List;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;

/**
 * 配置客户端接口：用于读取迁移配置与灰度规则。
 */
public interface ConfigClient extends AutoCloseable {

    /**
     * 查询迁移任务配置。
     *
     * @param migrationKey 迁移任务 key
     * @return 迁移配置
     */
    MigrationConfig getMigrationConfig(String migrationKey);

    /**
     * 查询灰度规则。
     *
     * @param migrationKey 迁移任务 key
     * @return 灰度规则列表
     */
    List<GrayscaleConfig> getGrayscaleRules(String migrationKey);

    @Override
    default void close() {
        // no-op
    }
}
