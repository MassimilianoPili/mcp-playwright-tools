package io.github.massimilianopili.mcp.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Lazy provider per le risorse Playwright. L'inizializzazione avviene alla prima
 * invocazione di un tool, non all'avvio di Spring, cosi' un browser mancante
 * non crasha l'ApplicationContext.
 *
 * <p>Thread-safe: tutto lo stato mutabile e' protetto da {@code synchronized}.
 * Auto-recovery: se l'inizializzazione fallisce, la prossima chiamata ritenta
 * (permette di installare i browser a runtime senza riavviare il server).</p>
 */
public class PlaywrightProvider implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightProvider.class);

    private final PlaywrightProperties props;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page currentPage;
    private String lastError;

    public PlaywrightProvider(PlaywrightProperties props) {
        this.props = props;
    }

    /**
     * Restituisce la pagina corrente, creando l'intero stack Playwright lazily
     * se necessario. Lancia {@link IllegalStateException} con messaggio descrittivo
     * se l'inizializzazione fallisce.
     */
    public synchronized Page getPage() {
        ensureInitialized();
        if (currentPage == null || currentPage.isClosed()) {
            currentPage = browserContext.newPage();
            log.debug("Nuova pagina creata");
        }
        return currentPage;
    }

    /**
     * Restituisce il BrowserContext, inizializzando lazily se necessario.
     * Usato dal tool tabs() che accede direttamente a context.pages().
     */
    public synchronized BrowserContext getBrowserContext() {
        ensureInitialized();
        return browserContext;
    }

    /**
     * Chiude la pagina corrente. Una nuova pagina verra' creata alla prossima
     * chiamata a {@link #getPage()}.
     */
    public synchronized void closePage() {
        if (currentPage != null && !currentPage.isClosed()) {
            currentPage.close();
        }
        currentPage = null;
    }

    /**
     * Restituisce true se lo stack Playwright e' inizializzato e funzionante.
     */
    public synchronized boolean isAvailable() {
        if (browserContext == null) {
            try {
                ensureInitialized();
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return browserContext != null;
    }

    /**
     * Restituisce l'ultimo errore di inizializzazione, o null se nessun errore.
     */
    public synchronized String getLastError() {
        return lastError;
    }

    private void ensureInitialized() {
        if (browserContext != null) {
            return;
        }

        cleanup();

        try {
            playwright = Playwright.create();
            log.info("Playwright runtime creato");

            BrowserType bt = switch (props.getBrowserType()) {
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> playwright.chromium();
            };

            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(props.isHeadless())
                    .setArgs(List.of(
                            "--disable-gpu",
                            "--disable-dev-shm-usage",
                            "--no-sandbox"
                    ));

            browser = bt.launch(options);
            log.info("Browser {} lanciato (headless={}, connected={})",
                    props.getBrowserType(), props.isHeadless(),
                    browser.isConnected() ? "si" : "no");

            browserContext = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(props.getViewportWidth(), props.getViewportHeight())
                    .setLocale(props.getLocale()));
            log.info("BrowserContext creato (viewport={}x{}, locale={})",
                    props.getViewportWidth(), props.getViewportHeight(), props.getLocale());

            lastError = null;

        } catch (Exception e) {
            lastError = buildErrorMessage(e);
            log.error("Inizializzazione Playwright fallita: {}", lastError);
            cleanup();
            throw new IllegalStateException(lastError, e);
        }
    }

    private String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Executable doesn't exist")
                || msg.contains("browserType.launch")
                || msg.contains("Failed to launch"))) {
            return "Browser Playwright non disponibile. I binari del browser non sono installati. "
                 + "Eseguire: mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI "
                 + "-Dexec.args=\"install --with-deps chromium\" "
                 + "nella directory mcp-playwright-tools, poi riprovare "
                 + "(auto-recovery, nessun riavvio necessario).";
        }
        return "Inizializzazione Playwright fallita: " + msg;
    }

    private void cleanup() {
        if (currentPage != null) {
            try { currentPage.close(); } catch (Exception ignored) {}
            currentPage = null;
        }
        if (browserContext != null) {
            try { browserContext.close(); } catch (Exception ignored) {}
            browserContext = null;
        }
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
            browser = null;
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
            playwright = null;
        }
    }

    @Override
    public void close() {
        cleanup();
        log.info("PlaywrightProvider chiuso");
    }
}
