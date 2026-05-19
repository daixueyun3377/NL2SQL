package io.github.daixueyun3377.nl2sql.semantic;

import java.util.Collections;
import java.util.List;

/**
 * 语义层上下文：与物理 Schema 分离的业务含义、JOIN、规则与 few-shot。
 */
public final class SemanticContext {

    private final String domain;
    private final List<String> tables;
    private final String relationHints;
    private final String businessRules;
    private final List<SqlExample> examples;

    public SemanticContext(String domain, List<String> tables, String relationHints,
                           String businessRules, List<SqlExample> examples) {
        this.domain = domain;
        this.tables = tables == null ? Collections.emptyList() : tables;
        this.relationHints = relationHints;
        this.businessRules = businessRules;
        this.examples = examples == null ? Collections.emptyList() : examples;
    }

    public static SemanticContext empty() {
        return new SemanticContext(null, Collections.emptyList(), null, null, Collections.emptyList());
    }

    public String getDomain() {
        return domain;
    }

    public List<String> getTables() {
        return tables;
    }

    public String getRelationHints() {
        return relationHints;
    }

    public String getBusinessRules() {
        return businessRules;
    }

    public List<SqlExample> getExamples() {
        return examples;
    }
}
