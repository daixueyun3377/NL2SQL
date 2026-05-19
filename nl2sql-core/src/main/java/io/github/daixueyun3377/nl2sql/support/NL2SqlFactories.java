package io.github.daixueyun3377.nl2sql.support;

import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.executor.JdbcSqlExecutor;
import io.github.daixueyun3377.nl2sql.llm.LlmConfig;
import io.github.daixueyun3377.nl2sql.llm.LlmPromptTemplate;
import io.github.daixueyun3377.nl2sql.llm.OpenAiCompatibleLlmClient;
import io.github.daixueyun3377.nl2sql.schema.JdbcSchemaProvider;
import io.github.daixueyun3377.nl2sql.schema.SchemaMode;
import io.github.daixueyun3377.nl2sql.semantic.ClasspathSemanticCatalog;
import io.github.daixueyun3377.nl2sql.semantic.MarkdownSemanticCatalog;
import io.github.daixueyun3377.nl2sql.semantic.SemanticCatalogProvider;
import io.github.daixueyun3377.nl2sql.validator.DefaultSqlValidator;

import javax.sql.DataSource;
import java.util.List;

/**
 * 默认组件装配工厂（便于编程式集成与 Starter 复用）。
 */
public final class NL2SqlFactories {

    private NL2SqlFactories() {
    }

    public static NL2SqlConfig buildConfig(DataSource dataSource,
                                           SchemaMode schemaMode,
                                           List<String> allowedTables,
                                           String customSchemaDdl,
                                           int maxRows,
                                           int timeoutSeconds,
                                           LlmConfig llmConfig,
                                           SemanticCatalogProvider semanticCatalog) {
        return NL2SqlConfig.builder()
                .schemaProvider(new JdbcSchemaProvider(dataSource))
                .llmClient(new OpenAiCompatibleLlmClient(llmConfig))
                .sqlValidator(new DefaultSqlValidator())
                .sqlExecutor(new JdbcSqlExecutor(dataSource))
                .semanticCatalog(semanticCatalog)
                .schemaMode(schemaMode)
                .allowedTables(allowedTables)
                .customSchemaDdl(customSchemaDdl)
                .maxRows(maxRows)
                .timeoutSeconds(timeoutSeconds)
                .build();
    }

    public static LlmPromptTemplate loadLlmPromptTemplate(String classpathResource, String filePath) {
        String markdown = ResourceLocations.loadText(classpathResource, filePath);
        return LlmPromptTemplate.parseMarkdown(markdown);
    }

    public static SemanticCatalogProvider markdownSemanticCatalog(String classpathResource, String filePath) {
        String markdown = ResourceLocations.loadText(classpathResource, filePath);
        return new MarkdownSemanticCatalog(markdown);
    }

    /** @deprecated 使用 {@link #markdownSemanticCatalog(String, String)} */
    public static SemanticCatalogProvider classpathSemanticCatalog(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            return null;
        }
        if (resourcePath.endsWith(".md")) {
            return markdownSemanticCatalog(resourcePath, null);
        }
        return new ClasspathSemanticCatalog(resourcePath);
    }
}
