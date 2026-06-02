package common.listeners;

import common.config.ConfigManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries failed tests up to {@code retry.count} times (config-driven, default 1).
 * <p>Shared by every module so flaky-test handling is uniform.</p>
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt = 0;
    private final int maxRetries = ConfigManager.getInstance().getInt("ui.retry.count", 1);

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < maxRetries) {
            attempt++;
            return true;
        }
        return false;
    }
}

