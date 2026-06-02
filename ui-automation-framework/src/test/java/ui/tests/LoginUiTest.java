package ui.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.LoginPage;

/**
 * Sample Selenium UI test demonstrating the POM + shared listener wiring.
 * <p>Groups: {@code ui}, {@code smoke}/{@code regression}.</p>
 */
public class LoginUiTest extends BaseUiTest {

    @Test(groups = {"ui", "smoke"})
    public void validLoginShowsInventory() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .loginAs("standard_user", "secret_sauce");

        Assert.assertTrue(login.isLoggedIn(), "User should land on the inventory page after valid login");
    }

    @Test(groups = {"ui", "regression"})
    public void invalidLoginShowsError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .loginAs("locked_out_user", "wrong_password");

        Assert.assertFalse(login.errorMessage().isEmpty(), "An error banner should be shown for invalid login");
    }
}

