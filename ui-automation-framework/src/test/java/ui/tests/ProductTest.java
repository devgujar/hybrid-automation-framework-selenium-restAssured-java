package ui.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.ProductPage;

/**
 * Selenium UI tests for the products / inventory page (post-login), demonstrating the
 * POM + shared listener wiring. Relies on {@link BaseUiTest} auto-login.
 * <p>Groups: {@code ui}, {@code smoke}.</p>
 */
public class ProductTest extends BaseUiTest {

    /** Verifies the browser/page title after a successful login. */
    @Test(groups = {"ui", "smoke"})
    public void testTitleAfterLogin() {
        String title = getDriver().getTitle();
        Assert.assertTrue(title.equalsIgnoreCase("Swag Labs"), "Title should contain 'Swag Labs'");
    }

    /** Verifies the products page header renders the expected branding. */
    @Test(groups = {"ui", "smoke"})
    public void testHeaderPresentAfterLogin() {
        ProductPage productPage = new ProductPage(getDriver());
        Assert.assertTrue(productPage.getHeaderText().equalsIgnoreCase("Swag Labs"), "Header should contain 'Swag Labs'");
    }

    /** Adds two products and asserts the cart badge count reflects them. */
    @Test(groups = {"ui", "smoke"})
    public void testAddProductToCart() {
        ProductPage productPage = new ProductPage(getDriver());

        // Add two products to cart
        productPage.addProductToCart("Sauce Labs Backpack");
        productPage.addProductToCart("Sauce Labs Bike Light");

        // check if the count is increased to 2 for cart value
        productPage.getCartCount();
        Assert.assertEquals(productPage.getCartCount(), "2", "Cart count should be 2");
    }

    /** Adds three products, removes one, and asserts the remaining cart count. */
    @Test(groups = {"ui", "smoke"})
    public void testAddRemoveProductsCart() {
        ProductPage productPage = new ProductPage(getDriver());

        // Add Three products to the cart
        productPage.addProductToCart("Sauce Labs Backpack");
        productPage.addProductToCart("Sauce Labs Bike Light");
        productPage.addProductToCart("Sauce Labs Bolt T-Shirt");

        // Remove One product from the cart
        productPage.removeProductFromCart("Sauce Labs Bike Light");

        // check if the count is 2 for the cart value
        productPage.getCartCount();
        Assert.assertEquals(productPage.getCartCount(), "2", "Cart count should be 2");

    }

    /** Adds five products, removes all five, and asserts the cart is empty (count zero). */
    @Test(groups = {"ui", "smoke"})
    public void testAddRemoveFiveProductsCartIsEmpty() {
        ProductPage productPage = new ProductPage(getDriver());

        String[] products = {
                "Sauce Labs Backpack",
                "Sauce Labs Bike Light",
                "Sauce Labs Bolt T-Shirt",
                "Sauce Labs Fleece Jacket",
                "Sauce Labs Onesie"
        };

        // Add all five products to the cart
        for (String product : products) {
            productPage.addProductToCart(product);
        }

        // Verify the cart badge reflects five products
        Assert.assertEquals(productPage.getCartCount(), "5", "Cart count should be 5 after adding five products");

        // Remove all five products from the cart
        for (String product : products) {
            productPage.removeProductFromCart(product);
        }

        // The badge disappears when the cart is empty, so the count reads as empty (zero)
        Assert.assertEquals(productPage.getCartCount(), "", "Cart count should be zero (badge absent) after removing all products");
    }

}

