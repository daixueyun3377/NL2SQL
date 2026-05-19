package io.github.daixueyun3377.nl2sql.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSqlExecutorTest {

    @Test
    void appendLimitWhenMissing() {
        String sql = JdbcSqlExecutor.appendLimitIfMissing("SELECT * FROM t", 50);
        assertEquals("SELECT * FROM t LIMIT 50", sql);
    }

    @Test
    void keepExistingLimit() {
        String sql = JdbcSqlExecutor.appendLimitIfMissing("SELECT * FROM t LIMIT 10", 50);
        assertEquals("SELECT * FROM t LIMIT 10", sql);
    }
}
