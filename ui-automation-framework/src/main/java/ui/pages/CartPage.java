package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ui.base.BasePage;
import ui.support.XPathStore;

/**
 * Sample Page Object (POM) for the SauceDemo login page.
 * <p>
 * Demonstrates the POM pattern: locators + intention-revealing actions, no assertions.
 * </p>
 */
public class CartPage extends BasePage {

    private final By firstName = XPathStore.by("INPUT_BY_ID", "first-name");
    private final By lastName = XPathStore.by("INPUT_BY_ID", "last-name");
    private final By postalCode = XPathStore.by("INPUT_BY_ID", "postal-code");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public void clickCheckout() {
        click(XPathStore.by("BUTTON_BY_ID", "checkout"));
    }

    public void clickContinueShopping() {
        click(XPathStore.by("INPUT_BY_ID", "continue"));
    }

    public void clickFinish() {
        click(XPathStore.by("BUTTON_BY_ID", "finish"));
    }

    public void clickCancel() {
        click(XPathStore.by("BUTTON_BY_ID", "cancel"));
    }

    public void addCustomerDetails(String first, String last, String postal) {
        sendKeys(firstName, first);
        sendKeys(lastName, last);
        sendKeys(postalCode, postal);
    }
}

