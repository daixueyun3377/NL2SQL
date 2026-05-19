package io.github.daixueyun3377.nl2sql.autoconfigure;

import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.llm.LlmConfig;
import io.github.daixueyun3377.nl2sql.llm.LlmPromptTemplate;
import io.github.daixueyun3377.nl2sql.semantic.SemanticCatalogProvider;
import io.github.daixueyun3377.nl2sql.support.NL2SqlFactories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(Nl2SqlProperties.class)
@ConditionalOnProperty(prefix = "nl2sql", name = "enabled", havingValue = "true")
public class Nl2SqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NL2SqlConfig nl2SqlConfig(Nl2SqlProperties properties, DataSource dataSource) {
        LlmConfig llmConfig = buildLlmConfig(properties);
        return NL2SqlFactories.buildConfig(
                dataSource,
                properties.getSchemaMode(),
                properties.getTables(),
                properties.getCustomSchemaDdl(),
                properties.getMaxRows(),
                properties.getTimeoutSeconds(),
                llmConfig,
                resolveSemanticCatalog(properties)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public NL2SqlEngine nl2SqlEngine(NL2SqlConfig config) {
        return NL2SqlEngine.create(config);
    }

    private static LlmConfig buildLlmConfig(Nl2SqlProperties properties) {
        Nl2SqlProperties.Llm llm = properties.getLlm();
        LlmPromptTemplate template = null;
        if (hasText(llm.getPromptFile()) || hasText(llm.getPromptResource())) {
            template = NL2SqlFactories.loadLlmPromptTemplate(llm.getPromptResource(), llm.getPromptFile());
        }
        return new LlmConfig(
                llm.getApiKey(),
                llm.getModel(),
                llm.getBaseUrl(),
                llm.getSystemPrompt(),
                template
        );
    }

    private static SemanticCatalogProvider resolveSemanticCatalog(Nl2SqlProperties properties) {
        String provider = properties.getSemantic().getProvider();
        if (!hasText(provider) || "none".equalsIgnoreCase(provider)) {
            return null;
        }
        if ("markdown".equalsIgnoreCase(provider)) {
            Nl2SqlProperties.Semantic semantic = properties.getSemantic();
            return NL2SqlFactories.markdownSemanticCatalog(
                    semantic.getMarkdownResource(),
                    semantic.getMarkdownFile()
            );
        }
        if ("classpath".equalsIgnoreCase(provider)) {
            return NL2SqlFactories.classpathSemanticCatalog(properties.getSemantic().getClasspathResource());
        }
        throw new IllegalStateException(
                "Unknown semantic provider: " + provider + ". Supported: none, markdown, classpath"
        );
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
