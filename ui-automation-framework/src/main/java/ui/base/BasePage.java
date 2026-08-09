package ui.base;

import common.ai.AiFailureAnalyzer;
import common.config.ConfigManager;
import common.reporting.ReportManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ui.support.XPathStore;

import java.time.Duration;

/**
 * Foundation for all Page Objects (Page Object Model).
 * <p>
 * Centralises the driver reference, an explicit wait and small reusable interactions
 * so concrete pages stay focused on locators and business actions.
 * </p>
 */
public abstract class BasePage {

    private static final Logger log = LoggerFactory.getLogger(BasePage.class);

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final ConfigManager config = ConfigManager.getInstance();

    /**
     * @param driver the WebDriver this page object operates on (bound to the current thread)
     */
    protected BasePage(WebDriver driver) {
        this.driver = driver;
        int timeout = config.getInt("ui.explicit.wait.seconds", 15);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    /**
     * Navigates to an absolute URL.
     * @param url the absolute URL to open
     */
    public void open(String url) {
        driver.get(url);
    }

    /**
     * Waits until an element located by the given locator is visible, then returns it.
     * @param locator the element locator
     * @return the visible {@link WebElement}
     */
    public WebElement visibilityOfElementLocated(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until an element is clickable, then clicks it.
     * <p>
     * When runtime self-healing is enabled ({@code auto.heal.enabled=true}) and the locator
     * cannot be resolved, the live DOM and failing locator are sent to
     * {@link AiFailureAnalyzer#healLocator(String, String)}; a corrected locator, if returned,
     * is retried once so the test can recover. If healing is disabled or unsuccessful, the
     * original failure propagates unchanged so failure analysis (if enabled) still runs.
     * </p>
     * @param locator the element locator
     */
    public void click(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator))
                    .click();
        } catch (TimeoutException | NoSuchElementException e) {
            By healed = tryHeal(locator);
            if (healed == null) {
                throw e;
            }
            wait.until(ExpectedConditions.elementToBeClickable(healed))
                    .click();
        }
    }

    /**
     * Best-effort runtime self-heal for a failing locator. Returns a corrected {@link By} when
     * self-healing is enabled and the AI proposes a usable XPath, otherwise {@code null}.
     * Fully null-safe: any error simply yields {@code null} so the original failure surfaces.
     */
    private By tryHeal(By locator) {
        if (!AiFailureAnalyzer.autoHealEnabled()) {
            return null;
        }
        try {
            String healedXpath = AiFailureAnalyzer.healLocator(locator.toString(), driver.getPageSource());
            if (healedXpath == null || healedXpath.isBlank()) {
                return null;
            }
            By healed = By.xpath(healedXpath);
            String message = "Auto-healed locator: " + locator + "  ->  " + healed;
            log.warn(message);
            if (ReportManager.current() != null) {
                ReportManager.current().warning("\uD83E\uDE79 " + message);
            }
            return healed;
        } catch (Exception ex) {
            log.warn("Auto-heal attempt failed for {}: {}", locator, ex.toString());
            return null;
        }
    }

    /**
     * Waits until an element is clickable, then clicks it.
     * @param element the element
     */
    public void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element))
                .click();
    }

    /**
     * Types text into a field after clearing it.
     * @param locator the input field locator
     * @param text    the text to type
     */
    public void sendKeys(By locator, String text) {
        WebElement element = visibilityOfElementLocated(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Reads the visible text of an element once it becomes visible.
     * @param locator the element locator
     * @return the element's visible text
     */
    public String getText(By locator) {
        WebElement element = visibilityOfElementLocated(locator);
        return element.getText();
    }

    public void waitForTitle(String title) {
        wait.until(ExpectedConditions.titleContains(title));
    }

    /**
     * Resolves an element by an arbitrary visible-text snippet via the {@code TEXT_ON_PAGE}
     * XPath template — handy for asserting that a given label is rendered.
     * @param textOnPage the visible text to locate
     * @return the matching {@link WebElement} once visible
     */
    public WebElement getTextOnPage(String textOnPage) {
        WebElement element = visibilityOfElementLocated(XPathStore.by("TEXT_ON_PAGE",textOnPage));
        return element;
    }


}

