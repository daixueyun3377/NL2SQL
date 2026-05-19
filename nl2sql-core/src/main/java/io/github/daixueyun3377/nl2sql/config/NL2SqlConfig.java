package io.github.daixueyun3377.nl2sql.config;

import io.github.daixueyun3377.nl2sql.executor.SqlExecutor;
import io.github.daixueyun3377.nl2sql.llm.LlmClient;
import io.github.daixueyun3377.nl2sql.schema.SchemaMode;
import io.github.daixueyun3377.nl2sql.schema.SchemaProvider;
import io.github.daixueyun3377.nl2sql.semantic.SemanticCatalogProvider;
import io.github.daixueyun3377.nl2sql.validator.SqlValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 引擎配置（Builder）。各扩展点可替换默认实现。
 */
public final class NL2SqlConfig {

    private final SchemaProvider schemaProvider;
    private final LlmClient llmClient;
    private final SqlValidator sqlValidator;
    private final SqlExecutor sqlExecutor;
    private final SemanticCatalogProvider semanticCatalog;
    private final SchemaMode schemaMode;
    private final int maxRows;
    private final int timeoutSeconds;
    private final List<String> allowedTables;
    private final String customSchemaDdl;

    private NL2SqlConfig(Builder builder) {
        this.schemaProvider = Objects.requireNonNull(builder.schemaProvider, "schemaProvider");
        this.llmClient = Objects.requireNonNull(builder.llmClient, "llmClient");
        this.sqlValidator = Objects.requireNonNull(builder.sqlValidator, "sqlValidator");
        this.sqlExecutor = Objects.requireNonNull(builder.sqlExecutor, "sqlExecutor");
        this.semanticCatalog = builder.semanticCatalog;
        this.schemaMode = builder.schemaMode == null ? SchemaMode.FULL : builder.schemaMode;
        this.maxRows = builder.maxRows > 0 ? builder.maxRows : 100;
        this.timeoutSeconds = builder.timeoutSeconds > 0 ? builder.timeoutSeconds : 30;
        this.allowedTables = builder.allowedTables == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(builder.allowedTables));
        this.customSchemaDdl = builder.customSchemaDdl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SchemaProvider getSchemaProvider() {
        return schemaProvider;
    }

    public LlmClient getLlmClient() {
        return llmClient;
    }

    public SqlValidator getSqlValidator() {
        return sqlValidator;
    }

    public SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public SemanticCatalogProvider getSemanticCatalog() {
        return semanticCatalog;
    }

    public SchemaMode getSchemaMode() {
        return schemaMode;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public List<String> getAllowedTables() {
        return allowedTables;
    }

    public String getCustomSchemaDdl() {
        return customSchemaDdl;
    }

    public static final class Builder {
        private SchemaProvider schemaProvider;
        private LlmClient llmClient;
        private SqlValidator sqlValidator;
        private SqlExecutor sqlExecutor;
        private SemanticCatalogProvider semanticCatalog;
        private SchemaMode schemaMode;
        private int maxRows = 100;
        private int timeoutSeconds = 30;
        private List<String> allowedTables;
        private String customSchemaDdl;

        public Builder schemaProvider(SchemaProvider schemaProvider) {
            this.schemaProvider = schemaProvider;
            return this;
        }

        public Builder llmClient(LlmClient llmClient) {
            this.llmClient = llmClient;
            return this;
        }

        public Builder sqlValidator(SqlValidator sqlValidator) {
            this.sqlValidator = sqlValidator;
            return this;
        }

        public Builder sqlExecutor(SqlExecutor sqlExecutor) {
            this.sqlExecutor = sqlExecutor;
            return this;
        }

        public Builder semanticCatalog(SemanticCatalogProvider semanticCatalog) {
            this.semanticCatalog = semanticCatalog;
            return this;
        }

        public Builder schemaMode(SchemaMode schemaMode) {
            this.schemaMode = schemaMode;
            return this;
        }

        public Builder maxRows(int maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder allowedTables(List<String> allowedTables) {
            this.allowedTables = allowedTables;
            return this;
        }

        public Builder customSchemaDdl(String customSchemaDdl) {
            this.customSchemaDdl = customSchemaDdl;
            return this;
        }

        public NL2SqlConfig build() {
            return new NL2SqlConfig(this);
        }
    }
}
