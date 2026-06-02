package common.listeners;

import common.config.ConfigManager;
import common.driver.DriverManager;
import common.reporting.ReportManager;
import common.utility.ScreenshotUtil;
import com.aventstack.extentreports.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * The single, shared TestNG listener reused across UI, API and hybrid suites.
 * <p>
 * <b>Why it lives in {@code common}:</b> reporting, logging and pass/fail handling are
 * cross-cutting concerns identical for every module. UI-specific behaviour (screenshots)
 * is handled defensively here via {@link ScreenshotUtil}, which is a no-op when the thread
 * has no WebDriver — so the very same class works for API tests without modification.
 * </p>
 *
 * <p>Register it once per suite (testng.xml {@code <listeners>}) or via
 * {@code @Listeners(TestListener.class)} on a module base class.</p>
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        ReportManager.init("target/extent-report.html");
        log.info("=== Suite STARTED: {} | env={} ===",
                suite.getName(), ConfigManager.getInstance().env());
    }

    @Override
    public void onFinish(ISuite suite) {
        ReportManager.flush();
        log.info("=== Suite FINISHED: {} ===", suite.getName());
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("Test context starting: {}", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        ReportManager.startTest(result.getMethod().getQualifiedName());
        log.info(">> START  {}", result.getMethod().getQualifiedName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<< PASS   {}", result.getMethod().getMethodName());
        ReportManager.current().log(Status.PASS, "Test passed");
        ReportManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("<< FAIL   {} : {}", result.getMethod().getMethodName(),
                String.valueOf(result.getThrowable()));
        if (ReportManager.current() != null) {
            ReportManager.current().log(Status.FAIL, result.getThrowable());

            // UI failures get a screenshot; API failures silently skip (no driver bound).
            if (DriverManager.hasDriver()) {
                String path = ScreenshotUtil.capture(result.getMethod().getMethodName());
                if (path != null) {
                    ReportManager.current().addScreenCaptureFromPath(path);
                }
            }
        }
        ReportManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("<< SKIP   {}", result.getMethod().getMethodName());
        if (ReportManager.current() != null) {
            ReportManager.current().log(Status.SKIP, "Test skipped");
        }
        ReportManager.unload();
    }
}

