package top.bulgat.migration.sdk.core.support;

/**
 * Marker exception used to indicate old method has already been invoked and failed.
 */
public class OldInvocationFailedException extends RuntimeException {

    private final Exception oldError;

    /**
     * Creates marker exception.
     *
     * @param oldError exception thrown by old method
     */
    public OldInvocationFailedException(Exception oldError) {
        super(oldError);
        this.oldError = oldError;
    }

    /**
     * Returns old method error.
     *
     * @return old method exception
     */
    public Exception getOldError() {
        return oldError;
    }
}
