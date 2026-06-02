package common.utility;

import common.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots for the current thread's WebDriver.
 * <p>
 * Lives in {@code common} but degrades gracefully: when no driver is bound to the
 * thread (e.g. a pure API test), {@link #capture(String)} simply returns {@code null}.
 * This is what lets a single shared listener serve both UI and API executions.
 * </p>
 */
public final class ScreenshotUtil {

    private static final String DIR = "target/screenshots";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {}

    /**
     * Saves a PNG screenshot of the current thread's driver.
     * @param name logical name (usually the test method name)
     * @return absolute path to the saved file, or {@code null} when no UI driver is present
     */
    public static String capture(String name) {
        WebDriver driver = DriverManager.get();
        if (!(driver instanceof TakesScreenshot)) {
            return null; // API/non-UI thread -> nothing to capture
        }
        try {
            Files.createDirectories(Paths.get(DIR));
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Paths.get(DIR, name + "_" + LocalDateTime.now().format(TS) + ".png");
            Files.copy(src.toPath(), dest);
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            return null;
        }
    }
}

