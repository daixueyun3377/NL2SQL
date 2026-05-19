package io.github.daixueyun3377.nl2sql.schema;

/**
 * CUSTOM 模式：使用调用方传入的 DDL 文本。
 */
public final class CustomSchemaProvider implements SchemaProvider {

    @Override
    public String getSchema(SchemaRequest request) {
        String ddl = request.getCustomSchemaDdl();
        if (ddl == null || ddl.trim().isEmpty()) {
            throw new IllegalArgumentException("CUSTOM schema mode requires customSchemaDdl");
        }
        return ddl.trim();
    }
}
