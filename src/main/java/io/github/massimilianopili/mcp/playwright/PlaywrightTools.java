package io.github.massimilianopili.mcp.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "mcp.playwright.enabled", havingValue = "true")
public class PlaywrightTools {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightTools.class);

    private final PlaywrightProvider provider;
    private final PlaywrightProperties props;

    public PlaywrightTools(PlaywrightProvider provider, PlaywrightProperties props) {
        this.provider = provider;
        this.props = props;
    }

    @Tool(name = "playwright_navigate",
          description = "Navigates to a URL in the browser. Returns final URL, title and status. "
                      + "Use waitUntil to control when navigation is complete.")
    public Map<String, Object> navigate(
            @ToolParam(description = "Full URL to visit (e.g. https://example.com)") String url,
            @ToolParam(description = "Wait event: load (default), domcontentloaded, networkidle", required = false) String waitUntil) {
        try {
            Page page = provider.getPage();
            Page.NavigateOptions opts = new Page.NavigateOptions().setTimeout(props.getTimeout());
            if (waitUntil != null) {
                opts.setWaitUntil(switch (waitUntil) {
                    case "domcontentloaded" -> com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED;
                    case "networkidle" -> com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE;
                    case "commit" -> com.microsoft.playwright.options.WaitUntilState.COMMIT;
                    default -> com.microsoft.playwright.options.WaitUntilState.LOAD;
                });
            }
            page.navigate(url, opts);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("url", page.url());
            result.put("title", page.title());
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore navigate: {}", e.getMessage());
            return Map.of("error", "Errore navigazione: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_navigate_back",
          description = "Goes back to the previous page in the browser history.")
    public Map<String, Object> navigateBack() {
        try {
            Page page = provider.getPage();
            page.goBack(new Page.GoBackOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("url", page.url());
            result.put("title", page.title());
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore navigate back: {}", e.getMessage());
            return Map.of("error", "Errore navigazione indietro: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_snapshot",
          description = "Captures the ARIA accessibility snapshot of the current page. "
                      + "Returns the accessibility tree as YAML-like structured text. "
                      + "Preferable to screenshot for content analysis without vision models.")
    public String snapshot() {
        try {
            Page page = provider.getPage();
            return page.locator("body").ariaSnapshot();
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("Errore snapshot: {}", e.getMessage());
            return "Errore snapshot: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_screenshot",
          description = "Captures a screenshot of the current page as a base64 PNG image. "
                      + "Use fullPage=true to capture the entire scrollable page.")
    public Map<String, Object> screenshot(
            @ToolParam(description = "true to capture the entire scrollable page, false for the visible viewport", required = false) Boolean fullPage) {
        try {
            Page page = provider.getPage();
            byte[] bytes = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(fullPage != null && fullPage));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("base64", Base64.getEncoder().encodeToString(bytes));
            result.put("size_bytes", bytes.length);
            result.put("url", page.url());
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore screenshot: {}", e.getMessage());
            return Map.of("error", "Errore screenshot: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_click",
          description = "Clicks on a page element identified by CSS selector or XPath. "
                      + "Examples: 'button#submit', 'a[href=\"/login\"]', '//button[text()=\"OK\"]'.")
    public Map<String, Object> click(
            @ToolParam(description = "CSS selector or XPath of the element to click") String selector) {
        try {
            Page page = provider.getPage();
            page.click(selector, new Page.ClickOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            result.put("url", page.url());
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore click: {}", e.getMessage());
            return Map.of("error", "Errore click su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_fill",
          description = "Fills an input/textarea field with the specified text. "
                      + "Clears the field before typing. Use for login forms, search, etc.")
    public Map<String, Object> fill(
            @ToolParam(description = "CSS selector or XPath of the input field") String selector,
            @ToolParam(description = "Text to enter in the field") String value) {
        try {
            Page page = provider.getPage();
            page.fill(selector, value, new Page.FillOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore fill: {}", e.getMessage());
            return Map.of("error", "Errore fill su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_select_option",
          description = "Selects an option from a <select> element (dropdown). "
                      + "The value can be the option's value attribute or its visible text.")
    public Map<String, Object> selectOption(
            @ToolParam(description = "CSS selector or XPath of the <select> element") String selector,
            @ToolParam(description = "Value (value attribute) or label of the option to select") String value) {
        try {
            Page page = provider.getPage();
            List<String> selected = page.selectOption(selector, value);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            result.put("selected", selected);
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore select: {}", e.getMessage());
            return Map.of("error", "Errore select su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_press_key",
          description = "Presses a key on the page. Supports special keys: "
                      + "Enter, Tab, Escape, ArrowDown, ArrowUp, Backspace, Delete, etc. "
                      + "Combinations: Control+A, Shift+Tab, Meta+C.")
    public Map<String, Object> pressKey(
            @ToolParam(description = "Key to press (e.g. Enter, Tab, Escape, Control+A)") String key) {
        try {
            Page page = provider.getPage();
            page.keyboard().press(key);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("key", key);
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore pressKey: {}", e.getMessage());
            return Map.of("error", "Errore pressKey '" + key + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_type",
          description = "Types text character by character. If a selector is provided, "
                      + "types into the specified element. Otherwise types into the page "
                      + "(useful after clicking on a field).")
    public Map<String, Object> type(
            @ToolParam(description = "Text to type") String text,
            @ToolParam(description = "CSS selector or XPath of the element (optional)", required = false) String selector) {
        try {
            Page page = provider.getPage();
            if (selector != null && !selector.isBlank()) {
                page.locator(selector).type(text);
            } else {
                page.keyboard().type(text);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("typed_length", text.length());
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore type: {}", e.getMessage());
            return Map.of("error", "Errore type: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_evaluate",
          description = "Executes JavaScript code in the page and returns the result. "
                      + "The expression is evaluated in the current page context. "
                      + "Example: 'document.title', 'document.querySelectorAll(\"a\").length'.")
    public String evaluate(
            @ToolParam(description = "JavaScript expression to evaluate") String expression) {
        try {
            Page page = provider.getPage();
            Object result = page.evaluate(expression);
            return result != null ? result.toString() : "null";
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("Errore evaluate: {}", e.getMessage());
            return "Errore evaluate: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_wait_for",
          description = "Waits for an element to appear on the page or for text to become visible. "
                      + "Specify at least one of selector or text. "
                      + "Useful after navigation or actions that trigger loading.")
    public Map<String, Object> waitFor(
            @ToolParam(description = "CSS selector or XPath of the element to wait for", required = false) String selector,
            @ToolParam(description = "Text to wait for on the page", required = false) String text,
            @ToolParam(description = "Timeout in milliseconds (default: configured timeout)", required = false) Integer timeout) {
        try {
            Page page = provider.getPage();
            int waitTimeout = timeout != null ? timeout : props.getTimeout();

            if (selector != null && !selector.isBlank()) {
                page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(waitTimeout));
                return Map.of("status", "ok", "found", "selector", "selector", selector);
            } else if (text != null && !text.isBlank()) {
                page.locator("text=" + text).first().waitFor(
                        new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(waitTimeout));
                return Map.of("status", "ok", "found", "text", "text", text);
            } else {
                return Map.of("error", "Specificare almeno uno tra selector e text");
            }
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore waitFor: {}", e.getMessage());
            return Map.of("error", "Errore waitFor: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_get_content",
          description = "Extracts text or HTML content from the page or a specific element. "
                      + "Without selector returns the entire page text. "
                      + "With selector returns the element's content.")
    public String getContent(
            @ToolParam(description = "CSS selector or XPath to extract specific content (optional, without = entire page)", required = false) String selector) {
        try {
            Page page = provider.getPage();
            if (selector != null && !selector.isBlank()) {
                return page.locator(selector).first().textContent();
            } else {
                return page.content();
            }
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("Errore getContent: {}", e.getMessage());
            return "Errore getContent: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_tabs",
          description = "Lists all open tabs (pages) in the browser with URL and title.")
    public List<Map<String, Object>> tabs() {
        try {
            var ctx = provider.getBrowserContext();
            List<Page> pages = ctx.pages();
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                Page p = pages.get(i);
                Map<String, Object> tab = new LinkedHashMap<>();
                tab.put("index", i);
                tab.put("url", p.url());
                tab.put("title", p.title());
                result.add(tab);
            }
            return result;
        } catch (IllegalStateException e) {
            return List.of(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Errore tabs: {}", e.getMessage());
            return List.of(Map.of("error", "Errore tabs: " + e.getMessage()));
        }
    }

    @Tool(name = "playwright_close",
          description = "Closes the current browser page. A new page will be "
                      + "created automatically on the next operation.")
    public Map<String, Object> close() {
        try {
            provider.closePage();
            return Map.of("status", "ok", "message", "Pagina chiusa");
        } catch (Exception e) {
            log.error("Errore close: {}", e.getMessage());
            return Map.of("error", "Errore close: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_resize",
          description = "Resizes the browser viewport to the specified dimensions.")
    public Map<String, Object> resize(
            @ToolParam(description = "Width in pixels") int width,
            @ToolParam(description = "Height in pixels") int height) {
        try {
            Page page = provider.getPage();
            page.setViewportSize(width, height);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("width", width);
            result.put("height", height);
            return result;
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            log.error("Errore resize: {}", e.getMessage());
            return Map.of("error", "Errore resize: " + e.getMessage());
        }
    }
}
