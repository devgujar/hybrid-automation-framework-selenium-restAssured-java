# hybrid-automation-framework-selenium-restAssured-java

UI + API automation in one place — **Selenium WebDriver 4** for the browser, **REST Assured**
for the services, **TestNG** for execution, and a shared **common** module that wires logging,
config, reporting and listeners across every layer.

---

## 1. Architecture

A Maven multi-module build with one shared foundation and three test layers:

```
                     +------------------------------+
                     |            common            |
                     |  config . driver . utility   |
                     |    listeners . reporting     |
                     +--------------+---------------+
                                    | depended on by all
        +---------------------------+---------------------------+
        v                           v                           v
+----------------+        +----------------+        +----------------------+
|      api       |        |       ui       |        |  integration-tests   |
|  REST Assured  |        |    Selenium    |        |        Hybrid        |
| base . clients |        | base . pages . |        |  BaseHybridTest +    |
| . core         |        | support.tests  |        |  UI x API scenarios  |
+----------------+        +----------------+        +----------+-----------+
       ^                          ^                            |
       +-------- reuses api (clients) + ui (browser) ----------+
```

**How the layers stay decoupled**

- UI code never imports REST Assured; API code never imports Selenium.
- Hybrid scenarios live only in `integration-tests` and call `api.clients.CustomerApiClient`
  directly for the service side.
- The bridge is **`BaseHybridTest`**: it **extends** `ui.base.BaseUiTest` (browser lifecycle) and
  **composes** `api.base.BaseApiTest` (RestAssured base URI + relaxed SSL) — both behaviours
  without multiple inheritance.

---

## 2. Module Layout

```
common/
  src/main/java/common/
    config/ConfigManager.java        Singleton, environment-aware config reader
    driver/DriverFactory.java        Factory: browser / headless / grid
    driver/DriverManager.java        ThreadLocal WebDriver (parallel-safe)
    listeners/TestListener.java      ITestListener + ISuiteListener (log + report + screenshot)
    listeners/RetryAnalyzer.java     Retries flaky tests (ui.retry.count)
    listeners/RetryTransformer.java  Applies RetryAnalyzer to every @Test
    reporting/ReportManager.java     ExtentReports (Singleton + ThreadLocal)
    utility/ScreenshotUtil.java      Full-page screenshots; no-op on API threads
  src/main/resources/
    apiBaseConfig.properties         API base config (classpath root)
    config/ui.qa.properties          UI env + browser/grid/retry defaults
    config/ui.stage.properties
    config/api.qa.properties         API endpoint + auth + SSL
    config/api.stage.properties
    log4j2.xml
    META-INF/services/org.testng.ITestNGListener   Auto-registers the shared listeners

api-automation-framework/
  src/main/java/api/
    base/BaseApiClient.java   base/BaseApiTest.java
    clients/CustomerApiClient.java
    core/AuthProvider.java    core/SpecFactory.java
  src/test/java/api/tests/CustomerApiApiTest.java

ui-automation-framework/
  src/main/java/ui/
    base/BaseUiTest.java             Driver lifecycle + auto-login (getDriver())
    base/BasePage.java               POM foundation (waits, click, sendKeys, getText)
    pages/LoginPage.java   pages/ProductPage.java   pages/CartPage.java
    support/XPathStore.java          Builds locators from named templates
  src/main/resources/xpaths/xpath.store.properties   Reusable parameterised XPath shapes
  src/test/java/ui/tests/LoginUiTest.java   ProductTest.java   E2ETest.java
  src/test/resources/suites/ui-suite.xml

integration-tests/
  src/main/java/common/base/BaseHybridTest.java      UI lifecycle + composed API setup
  src/test/java/hybrid/tests/CustomerHybridTest.java
  src/test/resources/suites/hybrid-suite.xml
```

---

## 3. Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Page Object Model** | `ui.pages.*` | Encapsulate locators + actions, no assertions |
| **Factory** | `DriverFactory` | Build configured WebDriver (browser / headless / grid) |
| **Singleton** | `ConfigManager`, `ReportManager` | One shared config / report source |
| **ThreadLocal holder** | `DriverManager` | One WebDriver per test thread |
| **Composition over inheritance** | `BaseHybridTest` | Inherit UI base, compose API base |
| **Template store** | `XPathStore` + `xpath.store.properties` | Externalised, reusable locator shapes |
| **IAnnotationTransformer** | `RetryTransformer` | Apply retry to every test globally |
| **ServiceLoader (SPI)** | `META-INF/services` | Auto-register listeners on every module |

---

## 4. Locators via XPathStore

Locator *shapes* live in `xpaths/xpath.store.properties`; pages pass only the values:

```properties
INPUT_BY_ID              = //input[contains(@id,'%s')]
ELEMENT_BY_TAG_ATTRIBUTE = //%s[contains(@%s,'%s')]
ELEMENT_BY_ID            = //*[contains(@id,'%s')]
ELEMENT_BY_TAG_TEXT      = //%s[contains(text(),'%s')]
```

```java
// LoginPage
private final By username = XPathStore.by("INPUT_BY_ID", "user-name");
// -> //input[contains(@id,'user-name')]
```

`XPathStore.buildXpath(key, args...)` trims the template and every argument before formatting,
so stray spaces never leak into a locator. Adding a new shape is a one-line properties change.

---

## 5. Common Module Usage

- **Listeners** — `TestListener` + `RetryTransformer` auto-registered via TestNG ServiceLoader,
  so the same logging / reporting / retry applies to UI, API and hybrid runs with no per-suite wiring.
- **Logging** — SLF4J + Log4j2 (`log4j2.xml`).
- **Config** — `ConfigManager` precedence: `-Dkey` system properties ->
  `config/api.<env>.properties` -> `config/ui.<env>.properties` -> `apiBaseConfig.properties`.
- **Reporting** — ExtentReports -> `target/extent-report.html`.
- **Screenshots** — `ScreenshotUtil` captures a full-page image on UI failures and embeds it
  inline in the report; it safely no-ops when a thread has no WebDriver (API tests).

---

## 6. TestNG Strategy

- **Suites:** `ui-suite.xml`, `hybrid-suite.xml` (the API module uses default class scanning).
- **Groups:** `ui`, `api`, `hybrid`, `smoke`, `regression`.
- **Parallelism:** `parallel="methods" thread-count="2"`; each thread gets its own driver from
  `DriverManager`, read through `getDriver()`.
- **Retry:** `RetryTransformer` applies `RetryAnalyzer` (`ui.retry.count`, default 1).

---

## 7. Hybrid Test Examples

`hybrid.tests.CustomerHybridTest extends BaseHybridTest`:

1. **Create data via API -> validate via API**, then drive the UI in the same test.
2. **Fetch data via API -> feed a UI assertion** (API is the source of truth).
3. **Clean up via API** in `@AfterClass` so runs stay idempotent.

The API side uses `CustomerApiClient` (`getCustomer`, `createCustomerIfNotExists`,
`deleteCustomerIfExists`, `getCustomerPayload`, ...); the UI side uses the `LoginPage` POM.

---

## 8. Example Test Usage

The same patterns across all three layers — concise, readable, assertion-focused.

**UI** — `ui/tests/LoginUiTest.java`

```java
public class LoginUiTest extends BaseUiTest {

  // Driver is created per-thread in BaseUiTest; read it via getDriver().
  @Test(groups = {"ui", "smoke"})
  public void validLoginShowsInventory() {
    LoginPage login = new LoginPage(getDriver())
        .openAt(baseUrl())
        .login();                          // default creds from config
    Assert.assertTrue(login.isLoggedIn(),
        "User should land on the inventory page");
  }

  @Test(groups = {"ui", "regression"})
  public void invalidLoginShowsError() {
    LoginPage login = new LoginPage(getDriver())
        .openAt(baseUrl())
        .login("locked_out_user", "wrong_password");
    Assert.assertFalse(login.errorMessage().isEmpty(),
        "An error banner should be shown");
  }
}
```

**API** — `api/tests/CustomerApiApiTest.java`

```java
public class CustomerApiApiTest extends BaseApiTest {

  CustomerApiClient client = new CustomerApiClient();

  @Test
  public void testCustomerCreate() throws Exception {
    String payload = client.getDefaultCustomerPayload();
    client.deleteCustomerIfExists("1");            // clean slate

    Response response = client.createCustomer(payload);
    Assert.assertEquals(response.getStatusCode(), 201);

    // Creating the same record again should conflict
    Response duplicate = client.createCustomer(payload);
    Assert.assertEquals(duplicate.getStatusCode(), 409);
  }
}
```

**Hybrid** — `hybrid/tests/CustomerHybridTest.java`

```java
// extends BaseHybridTest = UI lifecycle + composed API setup
public class CustomerHybridTest extends BaseHybridTest {

  private final CustomerApiClient customer = new CustomerApiClient();
  private static final String CUSTOMER_ID = "1000";

  @Test(groups = {"hybrid", "smoke"})
  public void createViaApi_thenValidate() throws Exception {
    // 1-2. Arrange + assert via API
    String payload = customer.getCustomerPayload(CUSTOMER_ID,
        "testUser1000", "testUser1000@mail.com",
        "India", "Pune", "411001");
    customer.createCustomerIfNotExists(payload, CUSTOMER_ID);
    Assert.assertTrue(customer.isCustomerRecordExists(CUSTOMER_ID));

    // 3. Drive the UI in the same test
    LoginPage login = new LoginPage(getDriver())
        .openAt(baseUrl())
        .login("standard_user", "secret_sauce");
    Assert.assertTrue(login.isLoggedIn());
  }

  @AfterClass(alwaysRun = true)
  public void cleanupData() {
    customer.deleteCustomerIfExists(CUSTOMER_ID);     // 4. Cleanup via API
  }
}
```

---

## 9. Configuration & Environments

| Scope | Keys |
|-------|------|
| UI | `ui.base.url`, `ui.default.userName`, `ui.default.password`, `ui.browser`, `ui.headless`, `ui.implicit.wait.seconds`, `ui.page.load.timeout.seconds`, `ui.explicit.wait.seconds`, `ui.grid.enabled`, `ui.grid.url`, `ui.retry.count` |
| API | `api.base.uri`, `api.ssl.relaxed`, `api.enable.logging`, `api.http.*`, `api.auth.*` |

- Select environment with `-Denv=qa|stage` (default `qa`).
- Override any key inline with `-Dkey=value`.
- Driver supports **Chrome / Firefox / Edge**, local or **Selenium Grid** (`ui.grid.enabled=true`).

---

## 10. Build & Run

```bash
mvn clean install -DskipTests                              # build all modules
mvn test -pl api-automation-framework                      # API layer
mvn test -pl ui-automation-framework -Dui.headless=true    # UI layer
mvn test -pl integration-tests -am -Dui.headless=true      # Hybrid layer

mvn test -pl ui-automation-framework -Dui.browser=firefox -Denv=stage   # overrides
```

Each `-pl` module maps cleanly onto a CI stage (GitHub Actions / Jenkins), one per layer.
The HTML report lands at `target/extent-report.html`; failure screenshots are embedded inline.

---

## 11. Best Practices Applied

- No duplicated API logic — hybrid tests call the API client directly.
- Minimal UI/API coupling — the only bridge is `common` plus `BaseHybridTest`.
- Parallel-ready by design — ThreadLocal driver, environment-driven config.
- Reusable, centralised locators, config, logging and reporting in `common`.

