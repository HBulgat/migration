package top.bulgat.migration.sdk.core.spi;

import top.bulgat.migration.sdk.core.model.DiffRequest;

/**
 * Diff 服务调用器接口。
 */
public interface DiffServiceCaller extends AutoCloseable {

    /**
     * 异步发送 Diff 请求。
     *
     * @param request diff 请求
     */
    void executeDiffAsync(DiffRequest request);

    @Override
    default void close() {
        // no-op
    }
}
