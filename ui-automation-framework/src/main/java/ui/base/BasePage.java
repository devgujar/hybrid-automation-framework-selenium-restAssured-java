package ui.base;

import common.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        int timeout = ConfigManager.getInstance().getInt("ui.explicit.wait.seconds", 15);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    /** Navigates to an absolute URL. */
    protected void open(String url) {
        driver.get(url);
    }

    /** Waits until an element located by the given locator is visible, then returns it. */
    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until an element is clickable, then clicks it. */
    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Types text into a field after clearing it. */
    protected void type(By locator, String text) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(text);
    }

    // ---------------------------------------------------------------------
    // Generic, reusable XPath locator builders.
    // Keep DRY: the same XPath shape is parameterised instead of duplicated
    // across page objects (e.g. only the id/attribute value changes).
    // ---------------------------------------------------------------------

    /**
     * Builds an XPath that matches any element whose attribute <i>contains</i> the given value.
     * @param tag       element tag (use {@code "*"} for any)
     * @param attribute attribute name (e.g. {@code id}, {@code data-test})
     * @param value     partial value to match via {@code contains()}
     * @return a {@link By} XPath locator like {@code //tag[contains(@attribute,'value')]}
     */
    protected By byAttributeContains(String tag, String attribute, String value) {
        return By.xpath(String.format("//%s[contains(@%s,'%s')]", tag, attribute, value));
    }

    /**
     * Convenience for the most common case: any element whose {@code id} contains the value.
     * @param idPart partial id value
     * @return XPath locator {@code //*[contains(@id,'idPart')]}
     */
    protected By byIdContains(String idPart) {
        return byAttributeContains("*", "id", idPart);
    }

    /**
     * Convenience for an {@code <input>} whose {@code id} contains the value.
     * @param idPart partial id value
     * @return XPath locator {@code //input[contains(@id,'idPart')]}
     */
    protected By inputByIdContains(String idPart) {
        return byAttributeContains("input", "id", idPart);
    }
}

