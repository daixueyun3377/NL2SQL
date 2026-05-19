package io.github.daixueyun3377.nl2sql.autoconfigure;

import io.github.daixueyun3377.nl2sql.schema.SchemaMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "nl2sql")
public class Nl2SqlProperties {

    private boolean enabled = false;
    private int maxRows = 100;
    private int timeoutSeconds = 30;
    private SchemaMode schemaMode = SchemaMode.FULL;
    private List<String> tables = new ArrayList<String>();
    private String customSchemaDdl;
    private final Llm llm = new Llm();
    private final Semantic semantic = new Semantic();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public SchemaMode getSchemaMode() {
        return schemaMode;
    }

    public void setSchemaMode(SchemaMode schemaMode) {
        this.schemaMode = schemaMode;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public String getCustomSchemaDdl() {
        return customSchemaDdl;
    }

    public void setCustomSchemaDdl(String customSchemaDdl) {
        this.customSchemaDdl = customSchemaDdl;
    }

    public Llm getLlm() {
        return llm;
    }

    public Semantic getSemantic() {
        return semantic;
    }

    public static class Llm {
        private String apiKey;
        private String model = "deepseek-chat";
        private String baseUrl = "https://api.deepseek.com/v1/";
        /** 行内 system，未配置 prompt-resource 时生效 */
        private String systemPrompt;
        /** classpath：nl2sql/llm-prompt.md */
        private String promptResource = "nl2sql/llm-prompt.md";
        /** 本地文件路径，优先级高于 prompt-resource */
        private String promptFile;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getPromptResource() {
            return promptResource;
        }

        public void setPromptResource(String promptResource) {
            this.promptResource = promptResource;
        }

        public String getPromptFile() {
            return promptFile;
        }

        public void setPromptFile(String promptFile) {
            this.promptFile = promptFile;
        }
    }

    public static class Semantic {
        /** none | markdown | classpath（json，兼容） */
        private String provider = "markdown";
        /** classpath：nl2sql/semantic-catalog.md */
        private String markdownResource = "nl2sql/semantic-catalog.md";
        /** 本地文件路径，优先级高于 markdown-resource */
        private String markdownFile;
        /** json 语义目录（provider=classpath 且非 .md 时使用） */
        private String classpathResource = "catalog/example-catalog.json";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getMarkdownResource() {
            return markdownResource;
        }

        public void setMarkdownResource(String markdownResource) {
            this.markdownResource = markdownResource;
        }

        public String getMarkdownFile() {
            return markdownFile;
        }

        public void setMarkdownFile(String markdownFile) {
            this.markdownFile = markdownFile;
        }

        public String getClasspathResource() {
            return classpathResource;
        }

        public void setClasspathResource(String classpathResource) {
            this.classpathResource = classpathResource;
        }
    }
}
