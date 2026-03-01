package io.github.massimilianopili.mcp.playwright;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.playwright")
public class PlaywrightProperties {

    private boolean enabled;
    private String browserType = "chromium";
    private boolean headless = true;
    private int timeout = 30000;
    private int viewportWidth = 1280;
    private int viewportHeight = 720;
    private String locale = "it-IT";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBrowserType() { return browserType; }
    public void setBrowserType(String browserType) { this.browserType = browserType; }

    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public int getViewportWidth() { return viewportWidth; }
    public void setViewportWidth(int viewportWidth) { this.viewportWidth = viewportWidth; }

    public int getViewportHeight() { return viewportHeight; }
    public void setViewportHeight(int viewportHeight) { this.viewportHeight = viewportHeight; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}
