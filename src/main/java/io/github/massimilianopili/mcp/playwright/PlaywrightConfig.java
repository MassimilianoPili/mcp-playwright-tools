package io.github.massimilianopili.mcp.playwright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "mcp.playwright.enabled", havingValue = "true")
@EnableConfigurationProperties(PlaywrightProperties.class)
public class PlaywrightConfig {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightConfig.class);

    @Bean(destroyMethod = "close")
    public PlaywrightProvider playwrightProvider(PlaywrightProperties props) {
        log.info("PlaywrightProvider creato (inizializzazione lazy al primo tool call)");
        return new PlaywrightProvider(props);
    }
}
