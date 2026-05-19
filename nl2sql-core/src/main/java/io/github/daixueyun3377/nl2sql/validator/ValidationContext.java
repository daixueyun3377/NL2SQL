package io.github.daixueyun3377.nl2sql.validator;

import java.util.Collections;
import java.util.List;

/**
 * 校验上下文：表白名单等。
 */
public final class ValidationContext {

    private final List<String> allowedTables;

    public ValidationContext(List<String> allowedTables) {
        this.allowedTables = allowedTables == null ? Collections.emptyList() : allowedTables;
    }

    public List<String> getAllowedTables() {
        return allowedTables;
    }
}
