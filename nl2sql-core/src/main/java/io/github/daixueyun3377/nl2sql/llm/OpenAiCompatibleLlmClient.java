package io.github.daixueyun3377.nl2sql.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;
import io.github.daixueyun3377.nl2sql.semantic.SqlExample;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Chat Completions 兼容客户端（DeepSeek 等）。
 * <p>
 * Prompt 优先使用 {@link LlmConfig#getPromptTemplate()}（应用方本地 MD 维护），否则回退内置逻辑。
 */
public final class OpenAiCompatibleLlmClient implements LlmClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是 MySQL SQL 生成助手。只输出一条可执行的 SELECT 语句，不要解释，不要 markdown。";

    private final LlmConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmConfig config) {
        this(config, new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build());
    }

    public OpenAiCompatibleLlmClient(LlmConfig config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateSql(SqlGenerateRequest request) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalStateException("LLM api-key is required");
        }
        LlmPromptTemplate template = config.getPromptTemplate();
        String userContent;
        String systemPrompt;
        if (template != null) {
            systemPrompt = template.getSystemPrompt();
            userContent = template.renderUserPrompt(request);
        } else {
            userContent = buildUserPrompt(request);
            systemPrompt = config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()
                    ? config.getSystemPrompt()
                    : DEFAULT_SYSTEM_PROMPT;
        }

        try {
            String body = buildRequestBody(systemPrompt, userContent);
            String url = normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions";
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IllegalStateException("LLM request failed: HTTP " + response.code());
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                String content = root.path("choices").path(0).path("message").path("content").asText("");
                return sanitizeSql(content);
            }
        } catch (IOException e) {
            throw new IllegalStateException("LLM request error", e);
        }
    }

    static String buildUserPrompt(SqlGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Database schema:\n").append(request.getSchema()).append("\n\n");
        SemanticContext semantic = request.getSemanticContext();
        if (semantic != null) {
            if (semantic.getRelationHints() != null && !semantic.getRelationHints().isEmpty()) {
                sb.append("JOIN hints:\n").append(semantic.getRelationHints()).append("\n\n");
            }
            if (semantic.getBusinessRules() != null && !semantic.getBusinessRules().isEmpty()) {
                sb.append("Business rules:\n").append(semantic.getBusinessRules()).append("\n\n");
            }
            if (!semantic.getExamples().isEmpty()) {
                sb.append("Examples:\n");
                for (SqlExample example : semantic.getExamples()) {
                    sb.append("Q: ").append(example.getQuestion()).append("\n");
                    sb.append("SQL: ").append(example.getSql()).append("\n");
                }
                sb.append("\n");
            }
        }
        sb.append("Question: ").append(request.getQuestion());
        return sb.toString();
    }

    private String buildRequestBody(String systemPrompt, String userContent) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.getModel());
        root.put("temperature", 0);
        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userContent);
        return objectMapper.writeValueAsString(root);
    }

    static String sanitizeSql(String raw) {
        if (raw == null) {
            return "";
        }
        String sql = raw.trim();
        if (sql.startsWith("```")) {
            int firstNewline = sql.indexOf('\n');
            if (firstNewline > 0) {
                sql = sql.substring(firstNewline + 1);
            }
            int fenceEnd = sql.lastIndexOf("```");
            if (fenceEnd >= 0) {
                sql = sql.substring(0, fenceEnd);
            }
        }
        return sql.trim().replaceAll(";\\s*$", "");
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "https://api.openai.com/v1";
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        if (normalized.endsWith("/v1/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
