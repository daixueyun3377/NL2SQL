package io.github.daixueyun3377.nl2sql.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 查询结果：成功时含 sql / rows；失败时含 error。
 */
public final class QueryResult {

    private final String sql;
    private final List<Map<String, Object>> rows;
    private final int rowCount;
    private final String error;
    private final long durationMs;

    private QueryResult(String sql, List<Map<String, Object>> rows, String error, long durationMs) {
        this.sql = sql;
        this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(rows);
        this.rowCount = this.rows.size();
        this.error = error;
        this.durationMs = durationMs;
    }

    public static QueryResult success(String sql, List<Map<String, Object>> rows, long durationMs) {
        return new QueryResult(sql, rows, null, durationMs);
    }

    public static QueryResult failure(String error, long durationMs) {
        return new QueryResult(null, null, error, durationMs);
    }

    public static QueryResult failure(String sql, String error, long durationMs) {
        return new QueryResult(sql, null, error, durationMs);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public String getSql() {
        return sql;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getError() {
        return error;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
