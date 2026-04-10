package tn.esprit.abonnement.config;

import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OpenAIConfig {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Bean
    @ConditionalOnProperty(name = "openai.enabled", havingValue = "true", matchIfMissing = false)
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-openai-api-key-here")) {
            log.warn("OpenAI API key not configured or is still placeholder. Chatbot will be disabled.");
            throw new IllegalStateException("OpenAI API key not configured. Set openai.enabled=false to disable chatbot or provide a valid API key.");
        }
        log.info("OpenAI service initialized with model: {}", model);
        return new OpenAiService(apiKey);
    }

    public String getModel() {
        return model;
    }
}
