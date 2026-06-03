package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ui.base.BasePage;
import ui.support.XPathStore;

/**
 * Page Object (POM) for the SauceDemo login page.
 * <p>
 * Demonstrates the POM pattern: locators + intention-revealing actions, no assertions.
 * Locators are resolved through {@link XPathStore} so their shapes live in one place.
 * </p>
 */
public class LoginPage extends BasePage {

    private final By username = XPathStore.by("INPUT_BY_ID", "user-name");
    private final By password = XPathStore.by("INPUT_BY_ID", "password");
    private final By loginButton = XPathStore.by("INPUT_BY_ID", "login-button");
    private final By errorBanner = XPathStore.by("ELEMENT_BY_TAG_ATTRIBUTE", "*", "data-test", "error");
    private final By inventoryContainer = XPathStore.by("ELEMENT_BY_ID", "inventory_container");

    /**
     * @param driver the WebDriver bound to the current thread
     */
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the login page at the given base URL.
     * @param baseUrl the UI application base URL
     * @return this page object for fluent chaining
     */
    public LoginPage openAt(String baseUrl) {
        open(baseUrl);
        return this;
    }

    /**
     * Performs a login with the supplied credentials.
     * @param user the username to submit
     * @param pass the password to submit
     * @return this page object for fluent chaining
     */
    public LoginPage login(String user, String pass) {
        sendKeys(username, user);
        sendKeys(password, pass);
        click(loginButton);
        return this;
    }

    /**
     * Performs a login with the default credentials from configuration
     * ({@code ui.default.userName} / {@code ui.default.password}).
     * @return this page object for fluent chaining
     */
    public LoginPage login() {
        login(config.get("ui.default.userName"), config.get("ui.default.password"));
        return this;
    }

    /**
     * Performs a login with the supplied user and the default password from configuration.
     * @param user the username to submit
     * @return this page object for fluent chaining
     */
    public LoginPage login(String user) {
        login(user, config.get("ui.default.password"));
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

