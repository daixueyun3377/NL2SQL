package io.github.daixueyun3377.nl2sql.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 从 Markdown 加载语义层目录（应用方在本地维护 {@code semantic-catalog.md}）。
 *
 * <p>格式见 {@code nl2sql/templates/semantic-catalog.template.md}。
 */
public final class MarkdownSemanticCatalog implements SemanticCatalogProvider {

    private final Map<String, DomainEntry> domains;
    private final DomainEntry defaultEntry;

    public MarkdownSemanticCatalog(String markdown) {
        ParsedCatalog parsed = parse(markdown);
        this.domains = Collections.unmodifiableMap(parsed.domains);
        this.defaultEntry = parsed.defaultEntry;
    }

    @Override
    public SemanticContext resolve(String question) {
        if (question == null || question.isEmpty()) {
            return toContext(defaultEntry);
        }
        String lower = question.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, DomainEntry> entry : domains.entrySet()) {
            if ("default".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            DomainEntry domain = entry.getValue();
            if (domain.matches(lower)) {
                return toContext(domain);
            }
        }
        return toContext(defaultEntry);
    }

    private static SemanticContext toContext(DomainEntry entry) {
        if (entry == null) {
            return SemanticContext.empty();
        }
        return new SemanticContext(
                entry.domain,
                entry.tables,
                entry.relationHints,
                entry.businessRules,
                entry.examples
        );
    }

    static ParsedCatalog parse(String markdown) {
        Map<String, DomainEntry> domains = new LinkedHashMap<String, DomainEntry>();
        DomainEntry current = null;
        String subsection = null;
        StringBuilder body = new StringBuilder();

        for (String line : markdown.split("\n", -1)) {
            if (line.startsWith("## ")) {
                flushSubsection(current, subsection, body);
                subsection = null;
                body.setLength(0);
                String name = line.substring(3).trim();
                current = new DomainEntry();
                current.sectionKey = name.toLowerCase(Locale.ROOT);
                domains.put(current.sectionKey, current);
                continue;
            }
            if (line.startsWith("### ")) {
                flushSubsection(current, subsection, body);
                subsection = line.substring(4).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
                body.setLength(0);
                continue;
            }
            if (current == null) {
                continue;
            }
            if (subsection == null && isMetaLine(line)) {
                applyMeta(current, line);
                continue;
            }
            if (subsection != null) {
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line);
            }
        }
        flushSubsection(current, subsection, body);

        DomainEntry defaultEntry = domains.get("default");
        return new ParsedCatalog(domains, defaultEntry);
    }

    private static boolean isMetaLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("domain:")
                || trimmed.startsWith("keywords:")
                || trimmed.startsWith("tables:");
    }

    private static void applyMeta(DomainEntry entry, String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return;
        }
        String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        String value = line.substring(colon + 1).trim();
        if ("domain".equals(key)) {
            entry.domain = value;
        } else if ("keywords".equals(key)) {
            entry.keywords = splitCsv(value);
        } else if ("tables".equals(key)) {
            entry.tables = splitCsv(value);
        }
    }

    private static void flushSubsection(DomainEntry entry, String subsection, StringBuilder body) {
        if (entry == null || subsection == null) {
            return;
        }
        String text = body.toString().trim();
        if ("relation_hints".equals(subsection)) {
            entry.relationHints = text;
        } else if ("business_rules".equals(subsection)) {
            entry.businessRules = text;
        } else if ("examples".equals(subsection)) {
            entry.examples = parseExamples(text);
        }
    }

    private static List<SqlExample> parseExamples(String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        List<SqlExample> examples = new ArrayList<SqlExample>();
        String currentQuestion = null;
        StringBuilder sql = new StringBuilder();
        boolean inCode = false;

        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("question:")) {
                if (currentQuestion != null && sql.length() > 0) {
                    examples.add(new SqlExample(currentQuestion, sql.toString().trim()));
                    sql.setLength(0);
                }
                currentQuestion = trimmed.substring("question:".length()).trim();
                inCode = false;
                continue;
            }
            if (trimmed.startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                if (sql.length() > 0) {
                    sql.append('\n');
                }
                sql.append(line);
            }
        }
        if (currentQuestion != null && sql.length() > 0) {
            examples.add(new SqlExample(currentQuestion, sql.toString().trim()));
        }
        return examples;
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = value.split(",");
        List<String> list = new ArrayList<String>();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                list.add(item);
            }
        }
        return list;
    }

    private static final class ParsedCatalog {
        private final Map<String, DomainEntry> domains;
        private final DomainEntry defaultEntry;

        private ParsedCatalog(Map<String, DomainEntry> domains, DomainEntry defaultEntry) {
            this.domains = domains;
            this.defaultEntry = defaultEntry;
        }
    }

    private static final class DomainEntry {
        private String sectionKey;
        private String domain;
        private List<String> keywords = Collections.emptyList();
        private List<String> tables = Collections.emptyList();
        private String relationHints;
        private String businessRules;
        private List<SqlExample> examples = Collections.emptyList();

        private boolean matches(String lowerQuestion) {
            if (sectionKey != null && !"default".equals(sectionKey) && lowerQuestion.contains(sectionKey)) {
                return true;
            }
            for (String keyword : keywords) {
                if (keyword != null && lowerQuestion.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }
}
