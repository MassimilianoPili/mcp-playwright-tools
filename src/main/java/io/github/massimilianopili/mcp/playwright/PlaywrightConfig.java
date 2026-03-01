package io.github.massimilianopili.mcp.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "mcp.playwright.enabled", havingValue = "true")
@EnableConfigurationProperties(PlaywrightProperties.class)
public class PlaywrightConfig {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightConfig.class);

    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        Playwright pw = Playwright.create();
        log.info("Playwright runtime creato");
        return pw;
    }

    @Bean(destroyMethod = "close")
    public Browser playwrightBrowser(Playwright pw, PlaywrightProperties props) {
        BrowserType bt = switch (props.getBrowserType()) {
            case "firefox" -> pw.firefox();
            case "webkit" -> pw.webkit();
            default -> pw.chromium();
        };

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(props.isHeadless())
                .setArgs(List.of(
                        "--disable-gpu",
                        "--disable-dev-shm-usage",
                        "--no-sandbox"
                ));

        Browser browser = bt.launch(options);
        log.info("Browser {} lanciato (headless={}, PID={})",
                props.getBrowserType(), props.isHeadless(), browser.isConnected() ? "connected" : "disconnected");
        return browser;
    }

    @Bean(destroyMethod = "close")
    public BrowserContext playwrightBrowserContext(Browser browser, PlaywrightProperties props) {
        BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(props.getViewportWidth(), props.getViewportHeight())
                .setLocale(props.getLocale()));
        log.info("BrowserContext creato (viewport={}x{}, locale={})",
                props.getViewportWidth(), props.getViewportHeight(), props.getLocale());
        return ctx;
    }
}
