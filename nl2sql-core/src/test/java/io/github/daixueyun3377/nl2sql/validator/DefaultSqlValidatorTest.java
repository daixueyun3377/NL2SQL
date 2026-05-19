package io.github.daixueyun3377.nl2sql.validator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSqlValidatorTest {

    private final DefaultSqlValidator validator = new DefaultSqlValidator();

    @Test
    void rejectsDelete() {
        assertThrows(SqlValidationException.class, () ->
                validator.validate("DELETE FROM job_basic_info", new ValidationContext(Arrays.asList("job_basic_info"))));
    }

    @Test
    void rejectsMultiStatement() {
        assertThrows(SqlValidationException.class, () ->
                validator.validate("SELECT 1; SELECT 2", new ValidationContext(null)));
    }

    @Test
    void rejectsComment() {
        assertThrows(SqlValidationException.class, () ->
                validator.validate("SELECT 1 -- comment", new ValidationContext(null)));
    }

    @Test
    void rejectsTableNotInWhitelist() {
        assertThrows(SqlValidationException.class, () ->
                validator.validate(
                        "SELECT * FROM secret_table",
                        new ValidationContext(Arrays.asList("job_basic_info"))
                ));
    }

    @Test
    void acceptsSelectOnWhitelistedTable() {
        assertDoesNotThrow(() ->
                validator.validate(
                        "SELECT id FROM job_basic_info WHERE status = 1",
                        new ValidationContext(Arrays.asList("job_basic_info"))
                ));
    }
}
