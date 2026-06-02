package ui.base;

import common.config.ConfigManager;
import common.driver.DriverFactory;
import common.driver.DriverManager;
import common.listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for all Selenium UI tests.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Spins up a thread-safe {@link WebDriver} through {@link DriverFactory} before each test
 *       and disposes it afterwards — enabling parallel execution.</li>
 *   <li>The shared {@link TestListener} (logging, reporting, screenshot-on-failure) is registered
 *       globally via TestNG ServiceLoader in {@code common}, so it applies here automatically
 *       without an {@code @Listeners} annotation. To pin it explicitly instead, add
 *       {@code @Listeners(TestListener.class)} on this class.</li>
 * </ul>
 * Hybrid tests extend this same class, so they reuse identical UI lifecycle handling.
 * </p>
 */
public abstract class BaseUiTest {

    protected final ConfigManager config = ConfigManager.getInstance();

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverFactory.createDriver();
    }

    /** @return the WebDriver bound to the current thread (parallel-safe). */
    protected WebDriver getDriver() {
        return DriverManager.get();
    }

    /** @return the UI application base URL for the active environment. */
    protected String baseUrl() {
        return config.get("ui.base.url");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quit();
    }


}

