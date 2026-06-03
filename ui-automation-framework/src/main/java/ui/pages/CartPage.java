package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ui.base.BasePage;
import ui.support.XPathStore;

/**
 * Page Object (POM) for the SauceDemo cart and checkout flow.
 * <p>
 * Demonstrates the POM pattern: locators + intention-revealing actions, no assertions.
 * Locators are resolved through {@link XPathStore} so their shapes live in one place.
 * </p>
 */
public class CartPage extends BasePage {

    private final By firstName = XPathStore.by("INPUT_BY_ID", "first-name");
    private final By lastName = XPathStore.by("INPUT_BY_ID", "last-name");
    private final By postalCode = XPathStore.by("INPUT_BY_ID", "postal-code");

    /**
     * @param driver the WebDriver bound to the current thread
     */
    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Starts checkout from the cart page. */
    public void clickCheckout() {
        click(XPathStore.by("BUTTON_BY_ID", "checkout"));
    }

    /** Continues to the next checkout step after entering customer details. */
    public void clickContinueShopping() {
        click(XPathStore.by("INPUT_BY_ID", "continue"));
    }

    /** Completes the order on the checkout overview page. */
    public void clickFinish() {
        click(XPathStore.by("BUTTON_BY_ID", "finish"));
    }

    /** Cancels the current checkout step. */
    public void clickCancel() {
        click(XPathStore.by("BUTTON_BY_ID", "cancel"));
    }

    /**
     * Fills in the checkout customer-information form.
     * @param first  customer first name
     * @param last   customer last name
     * @param postal postal / ZIP code
     */
    public void addCustomerDetails(String first, String last, String postal) {
        sendKeys(firstName, first);
        sendKeys(lastName, last);
        sendKeys(postalCode, postal);
    }
}

