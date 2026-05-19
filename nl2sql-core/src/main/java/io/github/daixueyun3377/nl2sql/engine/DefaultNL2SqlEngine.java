package io.github.daixueyun3377.nl2sql.engine;

import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.api.QueryResult;
import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.executor.ExecuteOptions;
import io.github.daixueyun3377.nl2sql.executor.SqlExecutor;
import io.github.daixueyun3377.nl2sql.llm.LlmClient;
import io.github.daixueyun3377.nl2sql.llm.SqlGenerateRequest;
import io.github.daixueyun3377.nl2sql.schema.SchemaRequest;
import io.github.daixueyun3377.nl2sql.schema.SchemaProvider;
import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;
import io.github.daixueyun3377.nl2sql.validator.SqlValidationException;
import io.github.daixueyun3377.nl2sql.validator.SqlValidator;
import io.github.daixueyun3377.nl2sql.validator.ValidationContext;

import java.util.List;
import java.util.Map;

/**
 * 默认引擎实现：SemanticContext → Schema → LLM → Validator → Executor。
 */
public final class DefaultNL2SqlEngine implements NL2SqlEngine {

    private final NL2SqlConfig config;
    private final SemanticContextResolver semanticResolver;

    public DefaultNL2SqlEngine(NL2SqlConfig config) {
        this.config = config;
        this.semanticResolver = new SemanticContextResolver(config.getSemanticCatalog());
    }

    @Override
    public QueryResult query(String question) {
        long start = System.currentTimeMillis();
        if (question == null || question.trim().isEmpty()) {
            return QueryResult.failure("question must not be blank", elapsed(start));
        }
        try {
            SemanticContext semanticContext = semanticResolver.resolve(question);
            SchemaProvider schemaProvider = config.getSchemaProvider();
            String schema = schemaProvider.getSchema(new SchemaRequest(
                    question,
                    config.getSchemaMode(),
                    config.getAllowedTables(),
                    config.getCustomSchemaDdl(),
                    semanticContext
            ));

            LlmClient llmClient = config.getLlmClient();
            String sql = llmClient.generateSql(new SqlGenerateRequest(question, schema, semanticContext));

            SqlValidator validator = config.getSqlValidator();
            validator.validate(sql, new ValidationContext(config.getAllowedTables()));

            SqlExecutor executor = config.getSqlExecutor();
            ExecuteOptions options = new ExecuteOptions(config.getMaxRows(), config.getTimeoutSeconds());
            List<Map<String, Object>> rows = executor.execute(sql, options);
            return QueryResult.success(sql, rows, elapsed(start));
        } catch (SqlValidationException e) {
            return QueryResult.failure(e.getMessage(), elapsed(start));
        } catch (RuntimeException e) {
            return QueryResult.failure(e.getMessage(), elapsed(start));
        }
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
