package io.github.daixueyun3377.nl2sql.validator;

/**
 * 扩展点：校验 LLM 生成的 SQL 安全性。
 */
public interface SqlValidator {

    void validate(String sql, ValidationContext context);
}
