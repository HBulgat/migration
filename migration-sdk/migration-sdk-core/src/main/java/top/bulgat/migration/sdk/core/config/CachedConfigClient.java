package top.bulgat.migration.sdk.core.config;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.sdk.core.model.GrayConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.spi.ConfigClient;

/**
 * 带有本地定时缓存和首次同步懒加载能力的 ConfigClient 装饰器。
 */
public class CachedConfigClient implements ConfigClient {

    private static final Logger log = LoggerFactory.getLogger(CachedConfigClient.class);

    private final ConfigClient delegate;
    private final ConcurrentHashMap<String, MigrationConfig> statusCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<GrayConfig>> rulesCache = new ConcurrentHashMap<>();
    private final Set<String> trackedKeys = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler;

    public CachedConfigClient(ConfigClient delegate, int refreshIntervalSeconds) {
        this.delegate = delegate;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "migration-config-refresher");
            t.setDaemon(true);
            return t;
        });

        // 启动后台定时轮询所有注册过的 key
        this.scheduler.scheduleAtFixedRate(
                this::refreshAll,
                refreshIntervalSeconds,
                refreshIntervalSeconds,
                TimeUnit.SECONDS);
    }

    @Override
    public MigrationConfig getMigrationConfig(String migrationKey) {
        trackedKeys.add(migrationKey);
        return statusCache.computeIfAbsent(migrationKey, key -> {
            try {
                return delegate.getMigrationConfig(key);
            } catch (Exception ex) {
                log.error("[Migration-SDK] Failed to lazy load status for {}, wait for next background refresh.", key,
                        ex);
                return null;
            }
        });
    }

    @Override
    public List<GrayConfig> getGrayRules(String migrationKey) {
        trackedKeys.add(migrationKey);
        return rulesCache.computeIfAbsent(migrationKey, key -> {
            try {
                return delegate.getGrayRules(key);
            } catch (Exception ex) {
                log.error("[Migration-SDK] Failed to lazy load gray rules for {}, wait for next background refresh.",
                        key, ex);
                return List.of();
            }
        });
    }

    private void refreshAll() {
        for (String key : trackedKeys) {
            try {
                MigrationConfig config = delegate.getMigrationConfig(key);
                if (config != null) {
                    statusCache.put(key, config);
                }
            } catch (Exception ex) {
                log.error("[Migration-SDK] Background refresh failed for migration status. key={}", key, ex);
            }

            try {
                List<GrayConfig> rules = delegate.getGrayRules(key);
                if (rules != null) {
                    rulesCache.put(key, rules);
                }
            } catch (Exception ex) {
                log.error("[Migration-SDK] Background refresh failed for gray rules. key={}", key, ex);
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        if (delegate instanceof AutoCloseable) {
            try {
                ((AutoCloseable) delegate).close();
            } catch (Exception ex) {
                log.error("[Migration-SDK] Failed to close delegate config client", ex);
            }
        }
    }
}
