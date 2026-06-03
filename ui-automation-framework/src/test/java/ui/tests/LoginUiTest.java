package ui.tests;

import common.driver.DriverFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.LoginPage;

/**
 * Selenium UI tests for the login flow, demonstrating the POM + shared listener wiring.
 * <p>Overrides {@link BaseUiTest#setUp()} to only create the driver (no auto-login), so each
 * test drives the login page explicitly.</p>
 * <p>Groups: {@code ui}, {@code smoke}/{@code regression}.</p>
 */
public class LoginUiTest extends BaseUiTest {

    /** Creates a fresh thread-bound driver without performing the default auto-login. */
    @Override
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverFactory.createDriver();
    }

    /** Expects login to fail (invalid username) and therefore not reach the inventory page. */
    @Test(groups = {"ui", "smoke"})
    public void FailLoginTest() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("standard_userddd", "secret_sauce");
        Assert.assertTrue(login.isLoggedIn(), "User should land on the inventory page after valid login");
    }

    /** Valid credentials should land the user on the inventory page. */
    @Test(groups = {"ui", "smoke"})
    public void validLoginShowsInventory() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login();

        Assert.assertTrue(login.isLoggedIn(), "User should land on the inventory page after valid login");
    }

    /** An invalid login should surface a visible error banner. */
    @Test(groups = {"ui", "regression"})
    public void invalidLoginShowsError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("locked_out_user", "wrong_password");

        Assert.assertFalse(login.errorMessage().isEmpty(), "An error banner should be shown for invalid login");
    }
}

