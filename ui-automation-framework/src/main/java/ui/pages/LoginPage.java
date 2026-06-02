package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ui.base.BasePage;

/**
 * Sample Page Object (POM) for the SauceDemo login page.
 * <p>
 * Demonstrates the POM pattern: locators + intention-revealing actions, no assertions.
 * </p>
 */
public class LoginPage extends BasePage {

    private final By username = inputByIdContains("user-name");
    private final By password = inputByIdContains("password");
    private final By loginButton = inputByIdContains("login-button");
    private final By errorBanner = byAttributeContains("*", "data-test", "error");
    private final By inventoryContainer = byIdContains("inventory_container");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /** Opens the login page at the given base URL. */
    public LoginPage openAt(String baseUrl) {
        open(baseUrl);
        return this;
    }

    /** Performs a login with the supplied credentials. */
    public LoginPage loginAs(String user, String pass) {
        type(username, user);
        type(password, pass);
        click(loginButton);
        return this;
    }

    /** @return {@code true} when the post-login inventory page is displayed. */
    public boolean isLoggedIn() {
        return !driver.findElements(inventoryContainer).isEmpty();
    }

    /** @return the validation error text, or empty string when none. */
    public String errorMessage() {
        var elements = driver.findElements(errorBanner);
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }
}

