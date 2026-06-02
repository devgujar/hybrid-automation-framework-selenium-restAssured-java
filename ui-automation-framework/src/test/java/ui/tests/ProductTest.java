package ui.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.ProductPage;

/**
 * Sample Selenium UI test demonstrating the POM + shared listener wiring.
 * <p>Groups: {@code ui}, {@code smoke}/{@code regression}.</p>
 */
public class ProductTest extends BaseUiTest {

    @Test(groups = {"ui", "smoke"})
    public void testTitleAfterLogin() {
        String title = getDriver().getTitle();
        Assert.assertTrue(title.equalsIgnoreCase("Swag Labs"), "Title should contain 'Swag Labs'");
    }

    @Test(groups = {"ui", "smoke"})
    public void testHeaderPresentAfterLogin() {
        ProductPage productPage = new ProductPage(getDriver());
        Assert.assertTrue(productPage.getHeaderText().equalsIgnoreCase("Swag Labs"), "Header should contain 'Swag Labs'");
    }

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

}

