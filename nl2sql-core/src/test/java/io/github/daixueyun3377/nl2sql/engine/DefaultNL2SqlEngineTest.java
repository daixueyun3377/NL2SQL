package io.github.daixueyun3377.nl2sql.engine;

import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.api.QueryResult;
import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.executor.JdbcSqlExecutor;
import io.github.daixueyun3377.nl2sql.llm.LlmClient;
import io.github.daixueyun3377.nl2sql.schema.SchemaMode;
import io.github.daixueyun3377.nl2sql.schema.SchemaProvider;
import io.github.daixueyun3377.nl2sql.validator.DefaultSqlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.h2.jdbcx.JdbcDataSource;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultNL2SqlEngineTest {

    private NL2SqlEngine engine;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:nl2sql;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        runSql(dataSource, "CREATE TABLE job_basic_info (id INT PRIMARY KEY, status INT)");
        runSql(dataSource, "INSERT INTO job_basic_info VALUES (1, 1), (2, 0)");

        SchemaProvider schemaProvider = request -> "CREATE TABLE job_basic_info (id INT, status INT);";
        LlmClient mockLlm = request -> "SELECT COUNT(*) AS cnt FROM job_basic_info WHERE status = 1";

        NL2SqlConfig config = NL2SqlConfig.builder()
                .schemaProvider(schemaProvider)
                .llmClient(mockLlm)
                .sqlValidator(new DefaultSqlValidator())
                .sqlExecutor(new JdbcSqlExecutor(dataSource))
                .schemaMode(SchemaMode.CUSTOM)
                .allowedTables(Arrays.asList("job_basic_info"))
                .maxRows(100)
                .timeoutSeconds(10)
                .build();

        engine = NL2SqlEngine.create(config);
    }

    @Test
    void queryReturnsRowsFromMockLlmSql() {
        QueryResult result = engine.query("how many active jobs");
        assertTrue(result.isSuccess(), result.getError());
        assertEquals("SELECT COUNT(*) AS cnt FROM job_basic_info WHERE status = 1", result.getSql());
        assertEquals(1, result.getRowCount());
        assertEquals(1L, ((Number) result.getRows().get(0).get("CNT")).longValue());
    }

    private static void runSql(JdbcDataSource dataSource, String sql) {
        try {
            dataSource.getConnection().createStatement().execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
