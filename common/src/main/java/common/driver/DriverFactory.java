package common.driver;

import common.config.ConfigManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory responsible for creating and configuring {@link WebDriver} instances.
 * <p>
 * <b>Design pattern:</b> Factory. Browser type, headless mode, grid usage and
 * timeouts are all driven by {@link ConfigManager}, keeping tests free of setup code.
 * </p>
 *
 * <p>Supported config keys (see {@code config/<env>.properties}):</p>
 * <ul>
 *   <li>{@code browser} = chrome | firefox | edge (default chrome)</li>
 *   <li>{@code headless} = true | false</li>
 *   <li>{@code grid.enabled} = true | false</li>
 *   <li>{@code grid.url} = http://localhost:4444/wd/hub</li>
 *   <li>{@code implicit.wait.seconds}, {@code page.load.timeout.seconds}</li>
 * </ul>
 */
public final class DriverFactory {

    private DriverFactory() {}

    /**
     * Creates a configured driver and binds it to the current thread via {@link DriverManager}.
     * @return the created {@link WebDriver}
     */
    public static WebDriver createDriver() {
        ConfigManager config = ConfigManager.getInstance();
        String browser = config.get("ui.browser", "chrome").trim().toLowerCase();
        boolean headless = resolveHeadless(config);
        boolean grid = config.getBoolean("ui.grid.enabled", false);

        WebDriver driver = grid ? createRemote(browser, headless, config)
                                : createLocal(browser, headless);

        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(config.getInt("ui.implicit.wait.seconds", 10)));
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(config.getInt("ui.page.load.timeout.seconds", 30)));
        if (!headless) {
            driver.manage().window().maximize();
        }

        DriverManager.set(driver);
        return driver;
    }

    private static WebDriver createLocal(String browser, boolean headless) {
        return switch (browser) {
            case "firefox" -> new FirefoxDriver(firefoxOptions(headless));
            case "edge"    -> new EdgeDriver(edgeOptions(headless));
            default        -> new ChromeDriver(chromeOptions(headless));
        };
    }

    private static WebDriver createRemote(String browser, boolean headless, ConfigManager config) {
        String gridUrl = config.get("ui.grid.url", "http://localhost:4444/wd/hub");
        try {
            return switch (browser) {
                case "firefox" -> new RemoteWebDriver(new URL(gridUrl), firefoxOptions(headless));
                case "edge"    -> new RemoteWebDriver(new URL(gridUrl), edgeOptions(headless));
                default        -> new RemoteWebDriver(new URL(gridUrl), chromeOptions(headless));
            };
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid grid.url: " + gridUrl, e);
        }
    }

    private static boolean resolveHeadless(ConfigManager config) {
        if (isCiEnvironment()) {
            return true;
        }
        return config.getBoolean("ui.headless", true);
    }

    private static boolean isCiEnvironment() {
        return "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"));
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new", "--disable-gpu");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        // Disable password manager popup
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--incognito");

        // Recommended prefs
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return options;
    }
}

