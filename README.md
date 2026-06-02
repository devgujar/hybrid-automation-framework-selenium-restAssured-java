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
 ├── common/                  Shared infra: config, driver, listeners, reporting, utility
 │      └── used by ───────────────┐                ┌──────────── used by
 │                                 ▼                ▼
 ├── api-automation-framework/  (EXISTING — unchanged)   ui-automation-framework/  (Selenium)
 │      api.base / api.clients / api.core                ui.base / ui.pages / ui.tests
 │                                 │                ▲
 │                                 └──── both reused by ────┐
 │                                                          ▼
 └── integration-tests/   Hybrid orchestration: BaseHybridTest + cross-layer UI/API tests
```

**How UI and API interact without tight coupling**

- UI code never imports REST Assured. Hybrid tests live in `integration-tests` and call the
  existing `api.clients.CustomerApiClient` **directly** for the API side.
- The bridge is **`BaseHybridTest`** (`integration-tests`, package `common.base`): it
  **extends** `ui.base.BaseUiTest` (full WebDriver lifecycle) and **composes**
  `api.base.BaseApiTest` (RestAssured base URI + relaxed SSL) — getting both behaviours without
  illegal multiple inheritance and without touching the API module.
- Result: **loose coupling**, **no duplicated API logic**, API module stays a black box.

---

## 2. Integration Strategy (key constraint)

> ❗ The API module must remain **unchanged**.

External consumption from the hybrid layer:

| Mode | Where | Example |
|------|-------|---------|
| Direct client call | hybrid test body / setup | `new CustomerApiClient().getCustomer(id)` |
| Reused API base setup | `common.base.BaseHybridTest` | composes `api.base.BaseApiTest#setUpBaseURI()` |

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
   config/ConfigManager.java          # Singleton, env-aware (UI + hybrid)
   driver/DriverFactory.java          # Factory (browser/grid/headless)
   driver/DriverManager.java          # ThreadLocal WebDriver (parallel-safe)
   listeners/TestListener.java        # Shared ITestListener + ISuiteListener
   listeners/RetryAnalyzer.java       # Flaky-test retry (ui.retry.count)
   listeners/RetryTransformer.java    # Auto-applies retry to all @Test
   reporting/ReportManager.java       # ExtentReports (Singleton + ThreadLocal)
   utility/ScreenshotUtil.java        # UI screenshots (no-op for API threads)
common/src/main/resources/
   apiBaseConfig.properties           # shared API base config (relocated from API module; classpath root)
   config/ui.qa.properties            # UI (Selenium) QA env + shared UI defaults
   config/ui.stage.properties         # UI (Selenium) Stage env
   config/api.qa.properties           # API (RestAssured) QA env
   config/api.stage.properties        # API (RestAssured) Stage env
   log4j2.xml
   META-INF/services/org.testng.ITestNGListener

api-automation-framework/   (UNCHANGED)  api.base / api.clients / api.core / api.config

ui-automation-framework/src/main/java/ui/
   base/{BaseUiTest,BasePage}.java     # driver lifecycle (getDriver) + POM foundation
   pages/LoginPage.java                # Page Object Model
ui-automation-framework/src/test/...    tests/LoginUiTest.java + suites/ui-suite.xml

integration-tests/  (hybrid)
   src/main/java/common/base/BaseHybridTest.java        # UI lifecycle + composed API setup
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
| **Composition over inheritance** | `BaseHybridTest` | Inherit UI base + compose API base |
| **IAnnotationTransformer** | `RetryTransformer` | Apply retry globally |

---

## 5. Common Module Usage

- ✅ **TestNG listeners** auto-registered via ServiceLoader.
- **Logging** — SLF4J + Log4j2 (`log4j2.xml`, console + `target/logs/automation.log`).
- **Config** — system props > `config/api.<env>.properties` > `config/ui.<env>.properties` > `apiBaseConfig.properties`.
- **Reporting** — ExtentReports → `target/extent-report.html`.
- **Screenshots** — `common.utility.ScreenshotUtil` attaches to the report on UI failures only.

---

## 6. TestNG Strategy

- **Suites:** `ui-suite.xml`, `hybrid-suite.xml` (API module keeps default class scanning).
- **Groups:** `smoke`, `regression`, `ui`, `api`, `hybrid`.
- **Parallelism:** `parallel="methods"` with a `ThreadLocal` driver per thread; tests read it via `getDriver()`.
- **Retry:** `RetryTransformer` applies `RetryAnalyzer` (`ui.retry.count`).

---

## 7. Hybrid Test Examples (`hybrid.tests.CustomerHybridTest extends BaseHybridTest`)

1. **Create data via API → validate via API**, then drive UI in the same test.
2. **Fetch data via API → feed UI validation** (API = source of truth).
3. **Cleanup via API** in `@AfterClass` — no API module changes.

---

## 8–10. Driver, Config, Reporting

- Thread-safe `DriverFactory` + `DriverManager` (Chrome/Firefox/Edge, local or **Selenium Grid**).
- Config keys (UI): `ui.browser`, `ui.headless`, `ui.grid.enabled`, `ui.grid.url`,
  `ui.implicit.wait.seconds`, `ui.page.load.timeout.seconds`, `ui.explicit.wait.seconds`, `ui.retry.count`, `ui.base.url`.
- Config keys (API): `api.base.uri`, `api.ssl.relaxed`, `api.enable.logging`, `api.auth.*`
  (resolved by the API module via `apiBaseConfig.properties`).
- Environments via `-Denv=qa|stage` (default `qa`); any key overridable with `-Dkey=value`.
- UI failures auto-attach a screenshot; API request/response logging stays in the API framework
  (`api.enable.logging` in `config/api.<env>.properties`).

---

## 11–12. CI/CD & Local Runs

Maven drives every layer; module selection keeps runs targeted and maps onto any CI runner
(GitHub Actions / Jenkins) with one stage per layer:

```bash
mvn clean install -DskipTests                              # build everything
mvn test -pl api-automation-framework                      # API layer (default scanning)
mvn test -pl ui-automation-framework -Dui.headless=true    # UI layer
mvn test -pl integration-tests -am -Dui.headless=true      # Hybrid layer
mvn test -pl ui-automation-framework -Dui.browser=firefox -Denv=qa   # overrides
```

---

## 13. Best Practices

- No duplication of API logic (direct client calls, API module untouched).
- Minimal UI↔API coupling (only via `common` + the `BaseHybridTest` bridge).
- Highly reusable `common` module; parallel-ready (ThreadLocal driver) and environment-driven.


