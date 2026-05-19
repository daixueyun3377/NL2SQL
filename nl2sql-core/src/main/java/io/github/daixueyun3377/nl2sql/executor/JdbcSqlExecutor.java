package io.github.daixueyun3377.nl2sql.executor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JDBC 执行器：查询超时 + 无 LIMIT 时追加 LIMIT maxRows。
 */
public final class JdbcSqlExecutor implements SqlExecutor {

    private final DataSource dataSource;

    public JdbcSqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Map<String, Object>> execute(String sql, ExecuteOptions options) {
        String executableSql = appendLimitIfMissing(sql, options.getMaxRows());
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (options.getTimeoutSeconds() > 0) {
                statement.setQueryTimeout(options.getTimeoutSeconds());
            }
            try (ResultSet rs = statement.executeQuery(executableSql)) {
                return mapRows(rs, options.getMaxRows());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL execution failed: " + e.getMessage(), e);
        }
    }

    static String appendLimitIfMissing(String sql, int maxRows) {
        if (maxRows <= 0) {
            return sql;
        }
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" limit ")) {
            return sql;
        }
        return sql + " LIMIT " + maxRows;
    }

    private static List<Map<String, Object>> mapRows(ResultSet rs, int maxRows) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        int count = 0;
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String label = meta.getColumnLabel(i);
                row.put(label, rs.getObject(i));
            }
            rows.add(row);
            count++;
            if (maxRows > 0 && count >= maxRows) {
                break;
            }
        }
        return rows;
    }
}
