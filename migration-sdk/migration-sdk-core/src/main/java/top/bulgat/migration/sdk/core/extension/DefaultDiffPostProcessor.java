package top.bulgat.migration.sdk.core.extension;

/**
 * 默认的对比后置处理器。
 * 默认行为为：不做任何转化，原样返回原始对象。
 */
public class DefaultDiffPostProcessor implements DiffPostProcessor {
    @Override
    public ProcessedResult process(String migrationKey, Object oldResult, Object newResult) {
        return new ProcessedResult(oldResult, newResult);
    }
}
