package common.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/**
 * Unified reporting facade shared by UI, API and hybrid tests.
 * <p>
 * <b>Design pattern:</b> Singleton ({@link ExtentReports}) + ThreadLocal ({@link ExtentTest})
 * so parallel tests log into their own node safely.
 * </p>
 * <p>Driven entirely by {@code common.listeners.TestListener}; test code never touches it directly.</p>
 */
public final class ReportManager {

    private static final ExtentReports EXTENT = new ExtentReports();
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();
    private static boolean initialised = false;

    private ReportManager() {}

    /** Attaches the HTML reporter once per JVM. Safe to call multiple times. */
    public static synchronized void init(String outputPath) {
        if (!initialised) {
            ExtentSparkReporter reporter = new ExtentSparkReporter(outputPath);
            reporter.config().setDocumentTitle("Hybrid Automation Report");
            reporter.config().setReportName("Selenium + REST Assured Hybrid Suite");
            EXTENT.attachReporter(reporter);
            initialised = true;
        }
    }

    /** Starts a report node for a test method and binds it to the current thread. */
    public static ExtentTest startTest(String name) {
        ExtentTest test = EXTENT.createTest(name);
        CURRENT.set(test);
        return test;
    }

    /** @return the report node for the current thread (may be {@code null}). */
    public static ExtentTest current() {
        return CURRENT.get();
    }

    /** Logs an informational step on the current node, if any. */
    public static void info(String message) {
        if (CURRENT.get() != null) {
            CURRENT.get().info(message);
        }
    }

    /** Flushes buffered results to disk. Called once at suite finish. */
    public static synchronized void flush() {
        EXTENT.flush();
    }

    /** Detaches the current thread's node to avoid leaks across reused threads. */
    public static void unload() {
        CURRENT.remove();
    }
}

