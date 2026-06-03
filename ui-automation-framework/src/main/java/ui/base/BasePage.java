package ui.base;

import common.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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
     * @param locator the element locator
     */
    public void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
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

