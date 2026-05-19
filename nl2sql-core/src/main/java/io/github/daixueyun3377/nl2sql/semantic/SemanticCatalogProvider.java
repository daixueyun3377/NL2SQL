package io.github.daixueyun3377.nl2sql.semantic;

/**
 * 扩展点：按问题解析语义层上下文。
 */
public interface SemanticCatalogProvider {

    SemanticContext resolve(String question);
}
