package io.github.daixueyun3377.nl2sql.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认 SQL 安全校验：仅 SELECT、表白名单、禁注释与多语句。
 */
public final class DefaultSqlValidator implements SqlValidator {

    private static final Pattern FORBIDDEN_KEYWORD = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|REPLACE|MERGE|GRANT|REVOKE|CALL|EXEC|EXECUTE)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TABLE_REF = Pattern.compile(
            "(?:FROM|JOIN)\\s+[`\"]?(\\w+)[`\"]?",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void validate(String sql, ValidationContext context) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlValidationException("SQL is empty");
        }
        String normalized = sql.trim();
        if (normalized.contains(";")) {
            throw new SqlValidationException("Multiple statements are not allowed");
        }
        if (normalized.contains("--") || normalized.contains("/*") || normalized.contains("*/")) {
            throw new SqlValidationException("SQL comments are not allowed");
        }
        if (!normalized.regionMatches(true, 0, "SELECT", 0, 6)) {
            throw new SqlValidationException("Only SELECT statements are allowed");
        }
        if (FORBIDDEN_KEYWORD.matcher(normalized).find()) {
            throw new SqlValidationException("Forbidden SQL keyword detected");
        }
        validateTables(normalized, context.getAllowedTables());
    }

    static void validateTables(String sql, List<String> allowedTables) {
        if (allowedTables == null || allowedTables.isEmpty()) {
            return;
        }
        Set<String> allowed = new HashSet<>();
        for (String table : allowedTables) {
            allowed.add(table.toLowerCase(Locale.ROOT));
        }
        Matcher matcher = TABLE_REF.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!allowed.contains(table)) {
                throw new SqlValidationException("Table not in whitelist: " + table);
            }
        }
    }
}
