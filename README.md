#     hybrid-automation-framework-selenium-restAssured-java
UI Automation Testing + Api Automation Testing Framework | Tech Stack - Selenium WebDriver + REST Assured + Java + TestNG 

A scalable, modular **hybrid** framework combining **Selenium WebDriver 4** (UI) with an
**existing REST Assured** API framework (reused **as-is, never modified**), orchestrated by **TestNG**
and glued together by a shared **common** module.

---

## 1. Architecture

```
root (pom) ──────────────────────────────────────────────────────────────
 │
 ├── common/                  Shared infra: config, driver, listeners, reporting
 │      └── used by ───────────────┐                ┌──────────── used by
 │                                 ▼                ▼
 ├── api-automation-framework/  (EXISTING — unchanged)   ui-automation-framework/  (Selenium)
 │      api.base / api.clients / api.core                ui.base / ui.pages / ui.tests
 │                                 │                ▲
 │                                 └──── both reused by ────┐
 │                                                          ▼
 └── integration-tests/   Hybrid orchestration: services (API facade) + UI + cross-layer tests
```

**How UI and API interact without tight coupling**

- UI never imports REST Assured directly. Hybrid tests talk to a **`CustomerService` facade**
  (`integration-tests/hybrid.services`) that wraps the existing `api.clients.CustomerApiClient`.
- The API module exposes plain `Response` methods; the facade adapts them into hybrid-friendly
  operations (`ensureCustomer`, `exists`, `fetchName`, `cleanup`).
- Result: **loose coupling**, **no duplicated API logic**, API module stays a black box.

---

## 2. Integration Strategy (key constraint)

> ❗ The API module must remain **unchanged**.

Two **external** consumption modes:

| Mode | Where | Example |
|------|-------|---------|
| Direct client call | hybrid test setup | `new CustomerApiClient().getCustomer(id)` |
| Service wrapper / facade | `hybrid.services.CustomerService` | `customerService.ensureCustomer(...)` |

**Listener reuse across UI *and* API without touching the API module** uses TestNG
**ServiceLoader auto-discovery**:

```
common/src/main/resources/META-INF/services/org.testng.ITestNGListener
    common.listeners.TestListener
    common.listeners.RetryTransformer
```

Because every module (including the untouched API module) depends on `common`, TestNG registers
these listeners automatically — **zero changes to API code or its test files**.

---

## 3. Folder Structure

```
common/src/main/java/common/
   config/ConfigManager.java          # Singleton, env-aware
   driver/DriverFactory.java          # Factory (browser/grid/headless)
   driver/DriverManager.java          # ThreadLocal WebDriver (parallel-safe)
   listeners/TestListener.java        # Shared ITestListener + ISuiteListener
   listeners/RetryAnalyzer.java       # Flaky-test retry
   listeners/RetryTransformer.java    # Auto-applies retry to all @Test
   reporting/ReportManager.java       # ExtentReports (Singleton + ThreadLocal)
   reporting/ScreenshotUtil.java      # UI screenshots (no-op for API threads)
common/src/main/resources/
   apiBaseConfig.properties           # shared API base config (relocated from API module; classpath root)
   config/ui.{qa,stage}.properties        # UI (Selenium) env config + shared UI defaults
   config/api.{qa,stage}.properties       # API (RestAssured) env config
   log4j2.xml
   META-INF/services/org.testng.ITestNGListener

api-automation-framework/   (UNCHANGED)  api.base / api.clients / api.core / api.config

ui-automation-framework/src/main/java/ui/
   base/BaseUiTest.java                # driver lifecycle + inherited listeners
   pages/{BasePage,LoginPage}.java     # Page Object Model
ui-automation-framework/src/test/...    tests/LoginUiTest.java + suites/ui-suite.xml

integration-tests/  (hybrid)
   src/main/java/hybrid/services/CustomerService.java   # API facade
   src/test/java/hybrid/tests/CustomerHybridTest.java + suites/hybrid-suite.xml
```

---

## 4. Design Patterns

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Page Object Model** | `ui.pages.*` | Encapsulate UI locators + actions |
| **Factory** | `DriverFactory` | Build configured WebDriver instances |
| **Singleton** | `ConfigManager`, `ReportManager` | One shared config/report source |
| **ThreadLocal holder** | `DriverManager` | Parallel-safe driver per thread |
| **IAnnotationTransformer** | `RetryTransformer` | Apply retry globally |

---

## 5. Common Module Usage

- ✅ **TestNG listeners** auto-registered via ServiceLoader.
- **Logging** — SLF4J + Log4j2 (`log4j2.xml`, console + `target/logs/automation.log`).
- **Config** — system props > `config/<env>.properties` > `config/config.properties`.
- **Reporting** — ExtentReports → `target/extent-report.html`.
- **Screenshots** — attached to the report on UI failures only.

---

## 6. TestNG Strategy

- **Suites:** `ui-suite.xml`, `hybrid-suite.xml` (API module keeps default scanning).
- **Groups:** `smoke`, `regression`, `ui`, `api`, `hybrid`.
- **Parallelism:** `parallel="methods"` with a `ThreadLocal` driver per thread.
- **Retry:** `RetryTransformer` applies `RetryAnalyzer` (`retry.count`).

---

## 7. Hybrid Test Examples (`hybrid.tests.CustomerHybridTest`)

1. **Create data via API → validate via API**, then drive UI in the same test.
2. **Fetch data via API → feed UI validation** (API = source of truth).
3. **Cleanup via API** in `@AfterClass` — no API module changes.

---

## 8–10. Driver, Config, Reporting

- Thread-safe `DriverFactory` + `DriverManager` (Chrome/Firefox/Edge, local or **Selenium Grid**).
- Config keys: `browser`, `headless`, `grid.enabled`, `grid.url`, timeouts, `retry.count`.
- Environments via `-Denv=qa|stage`; any key overridable with `-Dkey=value`.
- UI failures auto-attach a screenshot; API request/response logging stays in the API framework
  (`enable.logging` in `apiBaseConfig.properties`).

---

## 11–12. CI/CD & Local Runs

`.github/workflows/ci.yml` builds all modules then runs each layer:

```bash
mvn clean install -DskipTests                          # build everything
mvn test -pl api-automation-framework                  # API layer
mvn test -pl ui-automation-framework -Dheadless=true   # UI layer
mvn test -pl integration-tests -am -Dheadless=true     # Hybrid layer
mvn test -pl ui-automation-framework -Dbrowser=firefox -Denv=qa   # overrides
```

---

## 13. Best Practices

- No duplication of API logic (direct calls + facade).
- Minimal UI↔API coupling (only via `common` + thin service layer).
- Highly reusable `common` module; parallel-ready and environment-driven.


