package ui.tests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.CartPage;
import ui.pages.ProductPage;
import static org.testng.Assert.assertTrue;

/**
 * Sample Selenium UI test demonstrating the POM + shared listener wiring.
 * <p>Groups: {@code ui}, {@code smoke}/{@code regression}.</p>
 */
public class E2ETest extends BaseUiTest {
    private ProductPage productPage;
    private CartPage cartPage;

    @Override
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        super.setUp();
        productPage = new ProductPage(getDriver());
        cartPage = new CartPage(getDriver());
    }


    @Test(groups = {"ui", "regression"})
    public void testE2EAddProductsAndCheckOut() {
        // Add two Products
        productPage.addProductToCart("Sauce Labs Onesie");
        productPage.addProductToCart("Sauce Labs Bike Light");

        // click the cart button
        productPage.clickCart();
        assertTrue(productPage.getTextOnPage("Your Cart").isDisplayed());
        assertTrue(productPage.getTextOnPage("QTY").isDisplayed());

        // confirm Checkout
        cartPage.clickCheckout();
        assertTrue(productPage.getTextOnPage("Checkout: Your Information").isDisplayed());

        // Add customer details
        cartPage.addCustomerDetails("firstName","lastName","12345");
        cartPage.clickContinueShopping();
        assertTrue(productPage.getTextOnPage("Checkout: Overview").isDisplayed());

        // confirm Order
        cartPage.clickFinish();
        assertTrue(productPage.getTextOnPage("Thank you for your order!").isDisplayed());
    }


}

