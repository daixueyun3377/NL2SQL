package io.github.daixueyun3377.nl2sql.executor;

/**
 * SQL 执行选项。
 */
public final class ExecuteOptions {

    private final int maxRows;
    private final int timeoutSeconds;

    public ExecuteOptions(int maxRows, int timeoutSeconds) {
        this.maxRows = maxRows;
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
