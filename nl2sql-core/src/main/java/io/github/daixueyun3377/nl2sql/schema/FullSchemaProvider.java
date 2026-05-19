package io.github.daixueyun3377.nl2sql.schema;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FULL 模式：白名单表全量 DDL（information_schema.COLUMNS）。
 */
public final class FullSchemaProvider implements SchemaProvider {

    private final DataSource dataSource;

    public FullSchemaProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getSchema(SchemaRequest request) {
        List<String> tables = request.getAllowedTables();
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("FULL schema mode requires non-empty allowedTables");
        }
        try {
            return buildDdl(tables);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load schema from information_schema", e);
        }
    }

    String buildDdl(List<String> tables) throws SQLException {
        String schema;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            schema = resolveSchema(connection, meta);
        }

        Map<String, List<ColumnMeta>> tableColumns = new LinkedHashMap<>();
        for (String table : tables) {
            tableColumns.put(table.toLowerCase(Locale.ROOT), new ArrayList<ColumnMeta>());
        }

        String sql = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT "
                + "FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN (" + placeholders(tables.size()) + ") "
                + "ORDER BY TABLE_NAME, ORDINAL_POSITION";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, schema);
            for (String table : tables) {
                ps.setString(idx++, table);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT);
                    List<ColumnMeta> cols = tableColumns.get(tableName);
                    if (cols == null) {
                        continue;
                    }
                    cols.add(new ColumnMeta(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("COLUMN_TYPE"),
                            rs.getString("IS_NULLABLE"),
                            rs.getString("COLUMN_COMMENT")
                    ));
                }
            }
        }

        StringBuilder ddl = new StringBuilder();
        for (String table : tables) {
            List<ColumnMeta> columns = tableColumns.get(table.toLowerCase(Locale.ROOT));
            if (columns == null || columns.isEmpty()) {
                continue;
            }
            ddl.append("CREATE TABLE ").append(table).append(" (\n");
            for (int i = 0; i < columns.size(); i++) {
                ColumnMeta col = columns.get(i);
                ddl.append("  ").append(col.name).append(" ").append(col.type);
                if ("NO".equalsIgnoreCase(col.nullable)) {
                    ddl.append(" NOT NULL");
                }
                if (col.comment != null && !col.comment.isEmpty()) {
                    ddl.append(" COMMENT '").append(escapeComment(col.comment)).append("'");
                }
                if (i < columns.size() - 1) {
                    ddl.append(",");
                }
                ddl.append("\n");
            }
            ddl.append(");\n\n");
        }
        return ddl.toString().trim();
    }

    private static String resolveSchema(Connection connection, DatabaseMetaData meta) throws SQLException {
        String schema = connection.getSchema();
        if (schema != null && !schema.isEmpty()) {
            return schema;
        }
        String catalog = connection.getCatalog();
        if (catalog != null && !catalog.isEmpty()) {
            return catalog;
        }
        String product = meta.getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("h2")) {
            return "PUBLIC";
        }
        throw new IllegalStateException("Cannot resolve database schema for information_schema query");
    }

    private static String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    private static String escapeComment(String comment) {
        return comment.replace("'", "''");
    }

    private static final class ColumnMeta {
        private final String name;
        private final String type;
        private final String nullable;
        private final String comment;

        private ColumnMeta(String name, String type, String nullable, String comment) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.comment = comment;
        }
    }
}
