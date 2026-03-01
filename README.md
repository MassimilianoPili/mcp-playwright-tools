# MCP Playwright Tools

Spring Boot starter che fornisce 15 tool MCP per browser automation basati su [Playwright Java](https://playwright.dev/java/). Progettato per l'integrazione con [Spring AI](https://docs.spring.io/spring-ai/reference/) e il protocollo MCP.

## Caratteristiche

- **15 tool MCP** per navigazione, interazione, screenshot e analisi pagine web
- **Lazy initialization**: il browser si avvia solo al primo tool call, non allo startup dell'applicazione
- **Auto-recovery**: se il browser non e' installato, il server parte comunque. I tool tornano un errore descrittivo. Dopo l'installazione del browser, i tool funzionano alla prossima chiamata senza riavvio
- **Graceful degradation**: un errore Playwright non impatta gli altri tool MCP nel server

## Maven

```xml
<dependency>
    <groupId>io.github.massimilianopili</groupId>
    <artifactId>mcp-playwright-tools</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Tool disponibili

| Tool | Descrizione |
|------|-------------|
| `playwright_navigate` | Naviga a un URL, restituisce URL finale, titolo e stato |
| `playwright_navigate_back` | Torna alla pagina precedente |
| `playwright_snapshot` | Cattura lo snapshot ARIA di accessibilita' (albero strutturato) |
| `playwright_screenshot` | Screenshot PNG in base64 (viewport o full page) |
| `playwright_click` | Click su elemento (selettore CSS o XPath) |
| `playwright_fill` | Riempi campo input/textarea |
| `playwright_select_option` | Seleziona opzione da dropdown |
| `playwright_press_key` | Premi tasto (Enter, Tab, Escape, combinazioni) |
| `playwright_type` | Digita testo carattere per carattere |
| `playwright_evaluate` | Esegui JavaScript nella pagina |
| `playwright_wait_for` | Attendi elemento o testo visibile |
| `playwright_get_content` | Estrai contenuto testuale/HTML |
| `playwright_tabs` | Lista schede aperte |
| `playwright_close` | Chiudi pagina corrente |
| `playwright_resize` | Ridimensiona viewport |

## Configurazione

Attivazione tramite property Spring Boot:

```properties
mcp.playwright.enabled=true
```

Properties opzionali con valori di default:

| Property | Default | Descrizione |
|----------|---------|-------------|
| `mcp.playwright.enabled` | `false` | Attiva/disattiva la libreria |
| `mcp.playwright.browser-type` | `chromium` | Browser: `chromium`, `firefox`, `webkit` |
| `mcp.playwright.headless` | `true` | Modalita' headless |
| `mcp.playwright.timeout` | `30000` | Timeout operazioni (ms) |
| `mcp.playwright.viewport-width` | `1280` | Larghezza viewport (px) |
| `mcp.playwright.viewport-height` | `720` | Altezza viewport (px) |
| `mcp.playwright.locale` | `it-IT` | Locale browser |

Con variabili d'ambiente (per MCP server STDIO):

```properties
mcp.playwright.enabled=${MCP_PLAYWRIGHT_ENABLED:false}
mcp.playwright.browser-type=${MCP_PLAYWRIGHT_BROWSER:chromium}
mcp.playwright.headless=${MCP_PLAYWRIGHT_HEADLESS:true}
mcp.playwright.timeout=${MCP_PLAYWRIGHT_TIMEOUT:30000}
mcp.playwright.viewport-width=${MCP_PLAYWRIGHT_VIEWPORT_WIDTH:1280}
mcp.playwright.viewport-height=${MCP_PLAYWRIGHT_VIEWPORT_HEIGHT:720}
mcp.playwright.locale=${MCP_PLAYWRIGHT_LOCALE:it-IT}
```

## Installazione browser

I binari del browser devono essere installati separatamente:

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"
```

I binari vengono scaricati in `~/.cache/ms-playwright/` (~100 MB per Chromium).

Per impedire il download automatico a ogni avvio (consigliato in produzione):

```bash
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
```

## Architettura

```
PlaywrightToolsAutoConfiguration
  └── PlaywrightConfig (@ConditionalOnProperty "mcp.playwright.enabled=true")
        └── PlaywrightProvider (bean leggero, lazy init)
              └── PlaywrightTools (15 @Tool, inietta PlaywrightProvider)
```

- **PlaywrightProvider**: wrapper che crea Playwright → Browser → BrowserContext → Page al primo uso. Thread-safe (`synchronized`). Se l'init fallisce, ritenta alla prossima chiamata (auto-recovery)
- **PlaywrightTools**: 15 metodi `@Tool` che delegano a `provider.getPage()`. Ogni tool cattura `IllegalStateException` (browser non disponibile) e restituisce errore senza crash
- **PlaywrightConfig**: registra il bean `PlaywrightProvider` con `destroyMethod="close"` per cleanup allo shutdown

## Requisiti

- Java 21+
- Spring Boot 3.4.1
- Spring AI 1.0.0
- Playwright Java 1.50.0

## License

[MIT License](LICENSE)
