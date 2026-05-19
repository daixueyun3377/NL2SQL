package io.github.daixueyun3377.nl2sql.schema;

/**
 * 扩展点：提供供 LLM 使用的 Schema 文本。
 */
public interface SchemaProvider {

    String getSchema(SchemaRequest request);
}
