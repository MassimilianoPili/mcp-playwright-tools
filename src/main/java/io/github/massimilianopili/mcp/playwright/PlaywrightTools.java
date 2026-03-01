package io.github.massimilianopili.mcp.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
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

    private final BrowserContext browserContext;
    private final PlaywrightProperties props;
    private Page currentPage;

    public PlaywrightTools(BrowserContext browserContext, PlaywrightProperties props) {
        this.browserContext = browserContext;
        this.props = props;
    }

    private synchronized Page getOrCreatePage() {
        if (currentPage == null || currentPage.isClosed()) {
            currentPage = browserContext.newPage();
            log.debug("Nuova pagina creata");
        }
        return currentPage;
    }

    @Tool(name = "playwright_navigate",
          description = "Naviga a un URL nel browser. Restituisce URL finale, titolo e stato. "
                      + "Usa waitUntil per controllare quando la navigazione e' completa.")
    public Map<String, Object> navigate(
            @ToolParam(description = "URL completo da visitare (es. https://example.com)") String url,
            @ToolParam(description = "Evento di attesa: load (default), domcontentloaded, networkidle", required = false) String waitUntil) {
        try {
            Page page = getOrCreatePage();
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
        } catch (Exception e) {
            log.error("Errore navigate: {}", e.getMessage());
            return Map.of("error", "Errore navigazione: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_navigate_back",
          description = "Torna alla pagina precedente nella cronologia del browser.")
    public Map<String, Object> navigateBack() {
        try {
            Page page = getOrCreatePage();
            page.goBack(new Page.GoBackOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("url", page.url());
            result.put("title", page.title());
            return result;
        } catch (Exception e) {
            log.error("Errore navigate back: {}", e.getMessage());
            return Map.of("error", "Errore navigazione indietro: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_snapshot",
          description = "Cattura lo snapshot ARIA di accessibilita' della pagina corrente. "
                      + "Restituisce l'albero di accessibilita' come testo strutturato YAML-like. "
                      + "Preferibile allo screenshot per analisi del contenuto senza modelli di visione.")
    public String snapshot() {
        try {
            Page page = getOrCreatePage();
            return page.locator("body").ariaSnapshot();
        } catch (Exception e) {
            log.error("Errore snapshot: {}", e.getMessage());
            return "Errore snapshot: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_screenshot",
          description = "Cattura screenshot della pagina corrente come immagine PNG in base64. "
                      + "Usa fullPage=true per catturare l'intera pagina (scrollabile).")
    public Map<String, Object> screenshot(
            @ToolParam(description = "true per catturare l'intera pagina scrollabile, false per il viewport visibile", required = false) Boolean fullPage) {
        try {
            Page page = getOrCreatePage();
            byte[] bytes = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(fullPage != null && fullPage));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("base64", Base64.getEncoder().encodeToString(bytes));
            result.put("size_bytes", bytes.length);
            result.put("url", page.url());
            return result;
        } catch (Exception e) {
            log.error("Errore screenshot: {}", e.getMessage());
            return Map.of("error", "Errore screenshot: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_click",
          description = "Clicca su un elemento della pagina identificato da selettore CSS o XPath. "
                      + "Esempi: 'button#submit', 'a[href=\"/login\"]', '//button[text()=\"OK\"]'.")
    public Map<String, Object> click(
            @ToolParam(description = "Selettore CSS o XPath dell'elemento da cliccare") String selector) {
        try {
            Page page = getOrCreatePage();
            page.click(selector, new Page.ClickOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            result.put("url", page.url());
            return result;
        } catch (Exception e) {
            log.error("Errore click: {}", e.getMessage());
            return Map.of("error", "Errore click su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_fill",
          description = "Riempie un campo input/textarea con il testo specificato. "
                      + "Svuota il campo prima di scrivere. Usa per form di login, ricerca, etc.")
    public Map<String, Object> fill(
            @ToolParam(description = "Selettore CSS o XPath del campo input") String selector,
            @ToolParam(description = "Testo da inserire nel campo") String value) {
        try {
            Page page = getOrCreatePage();
            page.fill(selector, value, new Page.FillOptions().setTimeout(props.getTimeout()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            return result;
        } catch (Exception e) {
            log.error("Errore fill: {}", e.getMessage());
            return Map.of("error", "Errore fill su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_select_option",
          description = "Seleziona un'opzione da un elemento <select> (dropdown). "
                      + "Il valore puo' essere il value dell'option o il testo visibile.")
    public Map<String, Object> selectOption(
            @ToolParam(description = "Selettore CSS o XPath dell'elemento <select>") String selector,
            @ToolParam(description = "Valore (value attribute) o label dell'opzione da selezionare") String value) {
        try {
            Page page = getOrCreatePage();
            List<String> selected = page.selectOption(selector, value);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("selector", selector);
            result.put("selected", selected);
            return result;
        } catch (Exception e) {
            log.error("Errore select: {}", e.getMessage());
            return Map.of("error", "Errore select su '" + selector + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_press_key",
          description = "Premi un tasto sulla pagina. Supporta tasti speciali: "
                      + "Enter, Tab, Escape, ArrowDown, ArrowUp, Backspace, Delete, etc. "
                      + "Combinazioni: Control+A, Shift+Tab, Meta+C.")
    public Map<String, Object> pressKey(
            @ToolParam(description = "Tasto da premere (es. Enter, Tab, Escape, Control+A)") String key) {
        try {
            Page page = getOrCreatePage();
            page.keyboard().press(key);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("key", key);
            return result;
        } catch (Exception e) {
            log.error("Errore pressKey: {}", e.getMessage());
            return Map.of("error", "Errore pressKey '" + key + "': " + e.getMessage());
        }
    }

    @Tool(name = "playwright_type",
          description = "Digita testo carattere per carattere. Se fornito un selettore, "
                      + "digita nell'elemento specificato. Altrimenti digita nella pagina "
                      + "(utile dopo aver cliccato su un campo).")
    public Map<String, Object> type(
            @ToolParam(description = "Testo da digitare") String text,
            @ToolParam(description = "Selettore CSS o XPath dell'elemento (opzionale)", required = false) String selector) {
        try {
            Page page = getOrCreatePage();
            if (selector != null && !selector.isBlank()) {
                page.locator(selector).type(text);
            } else {
                page.keyboard().type(text);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("typed_length", text.length());
            return result;
        } catch (Exception e) {
            log.error("Errore type: {}", e.getMessage());
            return Map.of("error", "Errore type: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_evaluate",
          description = "Esegui codice JavaScript nella pagina e restituisci il risultato. "
                      + "L'espressione viene valutata nel contesto della pagina corrente. "
                      + "Esempio: 'document.title', 'document.querySelectorAll(\"a\").length'.")
    public String evaluate(
            @ToolParam(description = "Espressione JavaScript da valutare") String expression) {
        try {
            Page page = getOrCreatePage();
            Object result = page.evaluate(expression);
            return result != null ? result.toString() : "null";
        } catch (Exception e) {
            log.error("Errore evaluate: {}", e.getMessage());
            return "Errore evaluate: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_wait_for",
          description = "Attendi che un elemento appaia nella pagina o che un testo sia visibile. "
                      + "Specifica almeno uno tra selector e text. "
                      + "Utile dopo navigazione o azioni che causano caricamento.")
    public Map<String, Object> waitFor(
            @ToolParam(description = "Selettore CSS o XPath dell'elemento da attendere", required = false) String selector,
            @ToolParam(description = "Testo da attendere nella pagina", required = false) String text,
            @ToolParam(description = "Timeout in millisecondi (default: timeout configurato)", required = false) Integer timeout) {
        try {
            Page page = getOrCreatePage();
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
        } catch (Exception e) {
            log.error("Errore waitFor: {}", e.getMessage());
            return Map.of("error", "Errore waitFor: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_get_content",
          description = "Estrai il contenuto testuale o HTML dalla pagina o da un elemento specifico. "
                      + "Senza selettore restituisce il testo dell'intera pagina. "
                      + "Con selettore restituisce il contenuto dell'elemento.")
    public String getContent(
            @ToolParam(description = "Selettore CSS o XPath per estrarre contenuto specifico (opzionale, senza = intera pagina)", required = false) String selector) {
        try {
            Page page = getOrCreatePage();
            if (selector != null && !selector.isBlank()) {
                return page.locator(selector).first().textContent();
            } else {
                return page.content();
            }
        } catch (Exception e) {
            log.error("Errore getContent: {}", e.getMessage());
            return "Errore getContent: " + e.getMessage();
        }
    }

    @Tool(name = "playwright_tabs",
          description = "Lista tutte le schede (pagine) aperte nel browser con URL e titolo.")
    public List<Map<String, Object>> tabs() {
        try {
            List<Page> pages = browserContext.pages();
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                Page p = pages.get(i);
                Map<String, Object> tab = new LinkedHashMap<>();
                tab.put("index", i);
                tab.put("url", p.url());
                tab.put("title", p.title());
                tab.put("current", p == currentPage);
                result.add(tab);
            }
            return result;
        } catch (Exception e) {
            log.error("Errore tabs: {}", e.getMessage());
            return List.of(Map.of("error", "Errore tabs: " + e.getMessage()));
        }
    }

    @Tool(name = "playwright_close",
          description = "Chiudi la pagina corrente del browser. Una nuova pagina verra' "
                      + "creata automaticamente alla prossima operazione.")
    public Map<String, Object> close() {
        try {
            if (currentPage != null && !currentPage.isClosed()) {
                currentPage.close();
                currentPage = null;
                return Map.of("status", "ok", "message", "Pagina chiusa");
            }
            return Map.of("status", "ok", "message", "Nessuna pagina aperta");
        } catch (Exception e) {
            log.error("Errore close: {}", e.getMessage());
            currentPage = null;
            return Map.of("error", "Errore close: " + e.getMessage());
        }
    }

    @Tool(name = "playwright_resize",
          description = "Ridimensiona il viewport del browser alle dimensioni specificate.")
    public Map<String, Object> resize(
            @ToolParam(description = "Larghezza in pixel") int width,
            @ToolParam(description = "Altezza in pixel") int height) {
        try {
            Page page = getOrCreatePage();
            page.setViewportSize(width, height);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "ok");
            result.put("width", width);
            result.put("height", height);
            return result;
        } catch (Exception e) {
            log.error("Errore resize: {}", e.getMessage());
            return Map.of("error", "Errore resize: " + e.getMessage());
        }
    }
}
