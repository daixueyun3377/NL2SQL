package io.github.daixueyun3377.nl2sql.engine;

import io.github.daixueyun3377.nl2sql.semantic.SemanticCatalogProvider;
import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;

/**
 * 可选流水线步骤：解析动态语义层。
 */
public final class SemanticContextResolver {

    private final SemanticCatalogProvider semanticCatalog;

    public SemanticContextResolver(SemanticCatalogProvider semanticCatalog) {
        this.semanticCatalog = semanticCatalog;
    }

    public SemanticContext resolve(String question) {
        if (semanticCatalog == null) {
            return SemanticContext.empty();
        }
        SemanticContext context = semanticCatalog.resolve(question);
        return context == null ? SemanticContext.empty() : context;
    }
}
