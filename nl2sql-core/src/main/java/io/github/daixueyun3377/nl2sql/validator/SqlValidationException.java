package io.github.daixueyun3377.nl2sql.validator;

/**
 * SQL 校验失败。
 */
public class SqlValidationException extends RuntimeException {

    public SqlValidationException(String message) {
        super(message);
    }
}
