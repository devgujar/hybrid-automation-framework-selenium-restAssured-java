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

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        int timeout = config.getInt("ui.explicit.wait.seconds", 15);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    /** Navigates to an absolute URL. */
    public void open(String url) {
        driver.get(url);
    }

    /** Waits until an element located by the given locator is visible, then returns it. */
    public WebElement visibilityOfElementLocated(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until an element is clickable, then clicks it. */
    public void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Types text into a field after clearing it. */
    public void sendKeys(By locator, String text) {
        WebElement element = visibilityOfElementLocated(locator);
        element.clear();
        element.sendKeys(text);
    }

    public String getText(By locator) {
        WebElement element = visibilityOfElementLocated(locator);
        return element.getText();
    }

    public WebElement getTextOnPage(String textOnPage) {
        WebElement element = visibilityOfElementLocated(XPathStore.by("TEXT_ON_PAGE",textOnPage));
        return element;
    }


}

