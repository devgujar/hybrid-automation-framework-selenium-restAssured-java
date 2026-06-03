package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ui.base.BasePage;
import ui.support.XPathStore;

/**
 * Page Object (POM) for the SauceDemo products / inventory page.
 * <p>
 * Demonstrates the POM pattern: locators + intention-revealing actions, no assertions.
 * Locators are resolved through {@link XPathStore} so their shapes live in one place.
 * </p>
 */
public class ProductPage extends BasePage {

    private final By cart = XPathStore.by("ELEMENT_BY_CLASS", "shopping_cart_link");

    /**
     * @param driver the WebDriver bound to the current thread
     */
    public ProductPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Adds the named product to the cart by clicking its "Add to cart" button.
     * @param productName the visible product name (e.g. {@code "Sauce Labs Backpack"})
     */
    public void addProductToCart(String productName) {
        click(XPathStore.by("BUTTON_ADD_OR_REMOVE_FROM_CART", productName,"Add to cart"));
    }

    /**
     * Removes the named product from the cart by clicking its "Remove" button.
     * @param productName the visible product name (e.g. {@code "Sauce Labs Backpack"})
     */
    public void removeProductFromCart(String productName) {
        click(XPathStore.by("BUTTON_ADD_OR_REMOVE_FROM_CART", productName,"Remove"));
    }

    /** @return the page header text (the "Swag Labs" branding title). */
    public String getHeaderText() {
        return getText(XPathStore.by("ELEMENT_BY_TAG_TEXT", "div","Swag Labs"));
    }

    /** @return the cart badge count as text, or empty when the badge is absent. */
    public String getCartCount() {
        return getText(cart);
    }

    /** Opens the cart by clicking the shopping-cart icon. */
    public void clickCart() {
        click(cart);
    }

}

