package io.github.daixueyun3377.nl2sql.schema;

import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;

import java.util.Collections;
import java.util.List;

/**
 * Schema 请求上下文。
 */
public final class SchemaRequest {

    private final String question;
    private final SchemaMode mode;
    private final List<String> allowedTables;
    private final String customSchemaDdl;
    private final SemanticContext semanticContext;

    public SchemaRequest(String question, SchemaMode mode, List<String> allowedTables,
                         String customSchemaDdl, SemanticContext semanticContext) {
        this.question = question;
        this.mode = mode;
        this.allowedTables = allowedTables == null ? Collections.emptyList() : allowedTables;
        this.customSchemaDdl = customSchemaDdl;
        this.semanticContext = semanticContext;
    }

    public String getQuestion() {
        return question;
    }

    public SchemaMode getMode() {
        return mode;
    }

    public List<String> getAllowedTables() {
        return allowedTables;
    }

    public String getCustomSchemaDdl() {
        return customSchemaDdl;
    }

    public SemanticContext getSemanticContext() {
        return semanticContext;
    }
}
