package io.github.daixueyun3377.nl2sql.schema;

import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RELEVANT 模式：按问题 + {@link SemanticContext} 筛选表白名单子集后生成 DDL。
 */
public final class RelevantSchemaProvider implements SchemaProvider {

    private final FullSchemaProvider fullProvider;

    public RelevantSchemaProvider(FullSchemaProvider fullProvider) {
        this.fullProvider = fullProvider;
    }

    @Override
    public String getSchema(SchemaRequest request) {
        List<String> selected = selectTables(request);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("RELEVANT schema mode resolved to empty table set");
        }
        SchemaRequest narrowed = new SchemaRequest(
                request.getQuestion(),
                SchemaMode.FULL,
                selected,
                null,
                request.getSemanticContext()
        );
        return fullProvider.getSchema(narrowed);
    }

    List<String> selectTables(SchemaRequest request) {
        Set<String> selected = new LinkedHashSet<>();
        SemanticContext semantic = request.getSemanticContext();
        if (semantic != null && semantic.getTables() != null) {
            for (String table : semantic.getTables()) {
                if (isAllowed(table, request.getAllowedTables())) {
                    selected.add(table);
                }
            }
        }
        if (!selected.isEmpty()) {
            return new ArrayList<>(selected);
        }
        String question = request.getQuestion() == null ? "" : request.getQuestion().toLowerCase(Locale.ROOT);
        for (String table : request.getAllowedTables()) {
            if (question.contains(table.toLowerCase(Locale.ROOT))) {
                selected.add(table);
            }
        }
        if (!selected.isEmpty()) {
            return new ArrayList<>(selected);
        }
        int limit = Math.min(10, request.getAllowedTables().size());
        return new ArrayList<>(request.getAllowedTables().subList(0, limit));
    }

    private static boolean isAllowed(String table, List<String> allowedTables) {
        if (table == null) {
            return false;
        }
        String normalized = table.toLowerCase(Locale.ROOT);
        for (String allowed : allowedTables) {
            if (allowed != null && allowed.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
