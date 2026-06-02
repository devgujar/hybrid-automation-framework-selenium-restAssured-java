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
public class ProductPage extends BasePage {

    private final By cart = XPathStore.by("ELEMENT_BY_CLASS", "shopping_cart_link");

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public void addProductToCart(String productName) {
        click(XPathStore.by("BUTTON_ADD_OR_REMOVE_FROM_CART", productName,"Add to cart"));
    }

    public void removeProductFromCart(String productName) {
        click(XPathStore.by("BUTTON_ADD_OR_REMOVE_FROM_CART", productName,"Remove"));
    }

    public String getHeaderText() {
        return getText(XPathStore.by("ELEMENT_BY_TAG_TEXT", "div","Swag Labs"));
    }

    public String getCartCount() {
        return getText(cart);
    }

    public void clickCart() {
        click(cart);
    }

}

