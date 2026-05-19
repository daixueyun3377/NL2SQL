package io.github.daixueyun3377.nl2sql.llm;

/**
 * OpenAI 兼容 LLM 客户端配置。
 */
public final class LlmConfig {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    /** 行内覆盖 system（未配置 prompt 模板时生效） */
    private final String systemPrompt;
    private final LlmPromptTemplate promptTemplate;

    public LlmConfig(String apiKey, String model, String baseUrl, String systemPrompt) {
        this(apiKey, model, baseUrl, systemPrompt, null);
    }

    public LlmConfig(String apiKey, String model, String baseUrl, String systemPrompt,
                     LlmPromptTemplate promptTemplate) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.systemPrompt = systemPrompt;
        this.promptTemplate = promptTemplate;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public LlmPromptTemplate getPromptTemplate() {
        return promptTemplate;
    }
}
