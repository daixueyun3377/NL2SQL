package io.github.daixueyun3377.nl2sql.llm;

/**
 * 扩展点：调用 LLM 生成 SQL。
 */
public interface LlmClient {

    String generateSql(SqlGenerateRequest request);
}
