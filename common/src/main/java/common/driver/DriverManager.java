package common.driver;

import org.openqa.selenium.WebDriver;

/**
 * Thread-safe holder for the {@link WebDriver} bound to the current test thread.
 * <p>
 * Enables parallel UI execution: every TestNG thread gets its own driver instance,
 * so page objects, base tests and listeners can fetch "the current driver" without
 * passing it around explicitly.
 * </p>
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {}

    /** Binds a driver to the current thread. */
    public static void set(WebDriver driver) {
        DRIVER.set(driver);
    }

    /** @return the driver bound to the current thread, or {@code null} for non-UI (API) threads. */
    public static WebDriver get() {
        return DRIVER.get();
    }

    /** @return {@code true} when a driver is bound to the current thread. */
    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    /** Quits and unbinds the current thread's driver (no-op if none). */
    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}

