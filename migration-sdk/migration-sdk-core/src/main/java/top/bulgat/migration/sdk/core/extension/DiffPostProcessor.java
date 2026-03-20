package top.bulgat.migration.sdk.core.extension;

/**
 * 迁移数据对比后置处理器。
 * 允许用户在发送数据到 Diff 服务比对前，对旧接口和新接口的返回值进行清洗、平齐或脱敏。
 */
public interface DiffPostProcessor {

    /**
     * 执行数据后置处理。
     *
     * @param migrationKey 当前迁移标识
     * @param oldResult    旧接口原始返回值
     * @param newResult    新接口原始返回值
     * @return 处理后的结果容器，将直接用于 JSON 序列化和 Diff 比对
     */
    ProcessedResult process(String migrationKey, Object oldResult, Object newResult);

    /**
     * 被处理后的结果容器。
     *
     * @param processedOld 处理完毕的旧结果（将被作为旧版参照进行比对）
     * @param processedNew 处理完毕的新结果（将被作为新版差异进行比对）
     */
    record ProcessedResult(Object processedOld, Object processedNew) {
    }
}
