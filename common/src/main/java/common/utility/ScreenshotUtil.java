package common.utility;

import common.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures screenshots for the current thread's WebDriver.
 * <p>
 * Lives in {@code common} but degrades gracefully: when no driver is bound to the
 * thread (e.g. a pure API test), the capture methods simply return {@code null}.
 * This is what lets a single shared listener serve both UI and API executions.
 * </p>
 *
 * <p><b>Full-page capture strategy</b> (best available, with safe fallback):</p>
 * <ol>
 *   <li><b>Firefox</b> — native {@code getFullPageScreenshotAs}.</li>
 *   <li><b>Chrome / Edge</b> — DevTools (CDP) {@code Page.captureScreenshot} with
 *       {@code captureBeyondViewport=true} to grab the whole scrollable page.</li>
 *   <li><b>Otherwise</b> (e.g. remote Grid where CDP is unavailable) — falls back to a
 *       standard viewport screenshot so capturing never fails a test.</li>
 * </ol>
 */
public final class ScreenshotUtil {

    private static final String DIR = "target/screenshots";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {}

    /**
     * Saves a full-page PNG screenshot of the current thread's driver to {@code target/screenshots}.
     * @param name logical name (usually the test method name)
     * @return absolute path to the saved file, or {@code null} when no UI driver is present
     */
    public static String capture(String name) {
        String base64 = captureBase64();
        if (base64 == null) {
            return null; // API/non-UI thread -> nothing to capture
        }
        try {
            Files.createDirectories(Paths.get(DIR));
            Path dest = Paths.get(DIR, name + "_" + LocalDateTime.now().format(TS) + ".png");
            Files.write(dest, Base64.getDecoder().decode(base64));
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Captures a full-page screenshot of the current thread's driver as a Base64 string.
     * <p>
     * Preferred for embedding into HTML reports (e.g. ExtentReports): the image is
     * inlined directly into the report, so it always renders regardless of where the
     * report file is opened or moved — no broken absolute/relative path links.
     * </p>
     * @return Base64-encoded PNG, or {@code null} when no UI driver is present
     */
    public static String captureBase64() {
        WebDriver driver = DriverManager.get();
        if (!(driver instanceof TakesScreenshot)) {
            return null; // API/non-UI thread -> nothing to capture
        }

        // 1) Firefox: native full-page capture.
        if (driver instanceof FirefoxDriver firefox) {
            return firefox.getFullPageScreenshotAs(OutputType.BASE64);
        }

        // 2) Chrome / Edge: full page via DevTools (captures beyond the viewport).
        if (driver instanceof ChromiumDriver chromium) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("captureBeyondViewport", true);
                params.put("fromSurface", true);
                params.put("format", "png");
                Object result = chromium.executeCdpCommand("Page.captureScreenshot", params);
                if (result instanceof Map<?, ?> data && data.get("data") instanceof String png) {
                    return png;
                }
            } catch (RuntimeException ignored) {
                // CDP not available (e.g. remote Grid) -> fall through to viewport capture.
            }
        }

        // 3) Fallback: standard viewport screenshot (always works).
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }
}

