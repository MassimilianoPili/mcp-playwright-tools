package io.github.massimilianopili.mcp.playwright;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.playwright.enabled", havingValue = "true")
@Import({PlaywrightConfig.class, PlaywrightTools.class})
public class PlaywrightToolsAutoConfiguration {

    @Bean("playwrightToolCallbackProvider")
    public ToolCallbackProvider playwrightToolCallbackProvider(PlaywrightTools playwrightTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(playwrightTools)
                .build();
    }
}
