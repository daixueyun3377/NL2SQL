package io.github.daixueyun3377.nl2sql.schema;

import java.util.List;

/**
 * 基于 JDBC / information_schema 的 Schema 门面，按 {@link SchemaMode} 委托具体策略。
 */
public final class JdbcSchemaProvider implements SchemaProvider {

    private final FullSchemaProvider fullProvider;
    private final RelevantSchemaProvider relevantProvider;
    private final CustomSchemaProvider customProvider;

    public JdbcSchemaProvider(javax.sql.DataSource dataSource) {
        this.fullProvider = new FullSchemaProvider(dataSource);
        this.relevantProvider = new RelevantSchemaProvider(fullProvider);
        this.customProvider = new CustomSchemaProvider();
    }

    JdbcSchemaProvider(FullSchemaProvider fullProvider,
                       RelevantSchemaProvider relevantProvider,
                       CustomSchemaProvider customProvider) {
        this.fullProvider = fullProvider;
        this.relevantProvider = relevantProvider;
        this.customProvider = customProvider;
    }

    @Override
    public String getSchema(SchemaRequest request) {
        SchemaMode mode = request.getMode() == null ? SchemaMode.FULL : request.getMode();
        switch (mode) {
            case CUSTOM:
                return customProvider.getSchema(request);
            case RELEVANT:
                return relevantProvider.getSchema(request);
            case FULL:
            default:
                return fullProvider.getSchema(request);
        }
    }

    public FullSchemaProvider getFullProvider() {
        return fullProvider;
    }
}
