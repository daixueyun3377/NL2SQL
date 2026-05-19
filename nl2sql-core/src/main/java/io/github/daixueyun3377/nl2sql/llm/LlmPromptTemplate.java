package io.github.daixueyun3377.nl2sql.llm;

import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;
import io.github.daixueyun3377.nl2sql.semantic.SqlExample;
import io.github.daixueyun3377.nl2sql.support.TemplateRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * 从 Markdown 加载的 LLM Prompt 模板（System + User 两段）。
 */
public final class LlmPromptTemplate {

    private final String systemPrompt;
    private final String userPromptTemplate;

    public LlmPromptTemplate(String systemPrompt, String userPromptTemplate) {
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        this.userPromptTemplate = userPromptTemplate == null ? "" : userPromptTemplate.trim();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPromptTemplate() {
        return userPromptTemplate;
    }

    public String renderUserPrompt(SqlGenerateRequest request) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("schema", nullToEmpty(request.getSchema()));
        vars.put("question", nullToEmpty(request.getQuestion()));
        SemanticContext semantic = request.getSemanticContext();
        if (semantic == null) {
            vars.put("semantic.domain", "");
            vars.put("semantic.relationHints", "");
            vars.put("semantic.businessRules", "");
            vars.put("semantic.examples", "");
            vars.put("semantic.block", "");
        } else {
            vars.put("semantic.domain", nullToEmpty(semantic.getDomain()));
            vars.put("semantic.relationHints", nullToEmpty(semantic.getRelationHints()));
            vars.put("semantic.businessRules", nullToEmpty(semantic.getBusinessRules()));
            vars.put("semantic.examples", formatExamples(semantic));
            vars.put("semantic.block", formatSemanticBlock(semantic));
        }
        return TemplateRenderer.render(userPromptTemplate, vars);
    }

    /**
     * 解析 Markdown：{@code ## System} 与 {@code ## User} 两个章节。
     */
    public static LlmPromptTemplate parseMarkdown(String markdown) {
        String system = "";
        String user = "";
        String section = null;
        StringBuilder body = new StringBuilder();

        for (String line : markdown.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("## System")) {
                if ("system".equals(section)) {
                    system = body.toString().trim();
                } else if ("user".equals(section)) {
                    user = body.toString().trim();
                }
                section = "system";
                body.setLength(0);
                continue;
            }
            if (trimmed.equalsIgnoreCase("## User")) {
                if ("system".equals(section)) {
                    system = body.toString().trim();
                } else if ("user".equals(section)) {
                    user = body.toString().trim();
                }
                section = "user";
                body.setLength(0);
                continue;
            }
            if (section != null) {
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line);
            }
        }
        if ("system".equals(section)) {
            system = body.toString().trim();
        } else if ("user".equals(section)) {
            user = body.toString().trim();
        }
        if (system.isEmpty() || user.isEmpty()) {
            throw new IllegalArgumentException("LLM prompt markdown must contain non-empty ## System and ## User sections");
        }
        return new LlmPromptTemplate(system, user);
    }

    private static String formatSemanticBlock(SemanticContext semantic) {
        StringBuilder sb = new StringBuilder();
        if (semantic.getRelationHints() != null && !semantic.getRelationHints().isEmpty()) {
            sb.append("JOIN 说明：\n").append(semantic.getRelationHints()).append("\n\n");
        }
        if (semantic.getBusinessRules() != null && !semantic.getBusinessRules().isEmpty()) {
            sb.append("业务规则：\n").append(semantic.getBusinessRules()).append("\n\n");
        }
        String examples = formatExamples(semantic);
        if (!examples.isEmpty()) {
            sb.append("参考示例：\n").append(examples);
        }
        return sb.toString().trim();
    }

    private static String formatExamples(SemanticContext semantic) {
        if (semantic.getExamples() == null || semantic.getExamples().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (SqlExample example : semantic.getExamples()) {
            sb.append(i++).append(". Q: ").append(example.getQuestion()).append('\n');
            sb.append("   SQL: ").append(example.getSql()).append('\n');
        }
        return sb.toString().trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
