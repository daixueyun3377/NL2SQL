package io.github.daixueyun3377.nl2sql.llm;

import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;

/**
 * SQL 生成请求：问题、Schema 与可选语义上下文。
 */
public final class SqlGenerateRequest {

    private final String question;
    private final String schema;
    private final SemanticContext semanticContext;

    public SqlGenerateRequest(String question, String schema, SemanticContext semanticContext) {
        this.question = question;
        this.schema = schema;
        this.semanticContext = semanticContext;
    }

    public String getQuestion() {
        return question;
    }

    public String getSchema() {
        return schema;
    }

    public SemanticContext getSemanticContext() {
        return semanticContext;
    }
}
