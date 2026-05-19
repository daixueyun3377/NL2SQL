package io.github.daixueyun3377.nl2sql.semantic;

/**
 * Few-shot SQL 示例。
 */
public final class SqlExample {

    private final String question;
    private final String sql;

    public SqlExample(String question, String sql) {
        this.question = question;
        this.sql = sql;
    }

    public String getQuestion() {
        return question;
    }

    public String getSql() {
        return sql;
    }
}
