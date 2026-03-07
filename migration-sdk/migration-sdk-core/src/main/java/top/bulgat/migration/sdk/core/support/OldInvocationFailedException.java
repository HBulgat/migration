package top.bulgat.migration.sdk.core.support;

/**
 * 用于标记旧方法已执行且失败的异常。
 */
public class OldInvocationFailedException extends RuntimeException {

    private final Exception oldError;

    /**
     * 创建标记异常。
     *
     * @param oldError 旧方法抛出的异常
     */
    public OldInvocationFailedException(Exception oldError) {
        super(oldError);
        this.oldError = oldError;
    }

    /**
     * 返回旧方法异常。
     *
     * @return 旧方法 exception
     */
    public Exception getOldError() {
        return oldError;
    }
}
