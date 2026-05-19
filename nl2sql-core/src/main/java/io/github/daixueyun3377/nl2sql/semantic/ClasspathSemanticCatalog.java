package io.github.daixueyun3377.nl2sql.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 从 classpath YAML/JSON 加载语义目录（开发/测试用）。
 * <p>
 * 格式示例见 {@code resources/catalog/example-catalog.json}。
 */
public final class ClasspathSemanticCatalog implements SemanticCatalogProvider {

    private final Map<String, SemanticContext> domains;
    private final SemanticContext defaultContext;

    public ClasspathSemanticCatalog(String resourcePath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = ClasspathSemanticCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Semantic catalog resource not found: " + resourcePath);
            }
            JsonNode root = mapper.readTree(in);
            this.domains = parseDomains(root.path("domains"));
            this.defaultContext = parseContext(root.path("default"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load semantic catalog: " + resourcePath, e);
        }
    }

    @Override
    public SemanticContext resolve(String question) {
        if (question == null || question.isEmpty()) {
            return defaultContext == null ? SemanticContext.empty() : defaultContext;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, SemanticContext> entry : domains.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return defaultContext == null ? SemanticContext.empty() : defaultContext;
    }

    private static Map<String, SemanticContext> parseDomains(JsonNode domainsNode) {
        if (domainsNode == null || !domainsNode.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, SemanticContext> result = new java.util.LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = domainsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            result.put(field.getKey(), parseContext(field.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static SemanticContext parseContext(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return SemanticContext.empty();
        }
        List<String> tables = readStringList(node.path("tables"));
        List<SqlExample> examples = new ArrayList<>();
        JsonNode examplesNode = node.path("examples");
        if (examplesNode.isArray()) {
            for (JsonNode ex : examplesNode) {
                examples.add(new SqlExample(
                        text(ex, "question"),
                        text(ex, "sql")
                ));
            }
        }
        return new SemanticContext(
                text(node, "domain"),
                tables,
                text(node, "relationHints"),
                text(node, "businessRules"),
                examples
        );
    }

    private static List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode item : node) {
            list.add(item.asText());
        }
        return list;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
