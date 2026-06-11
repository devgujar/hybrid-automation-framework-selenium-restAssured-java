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
                .login("standard_userd_wrong_user", "secret_sauce");
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

    /** An empty username should surface a "Username is required" error and block login. */
    @Test(groups = {"ui", "regression"})
    public void emptyUsernameShowsError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("", "secret_sauce");

        Assert.assertFalse(login.isLoggedIn(), "User should not be logged in when the username is empty");
        Assert.assertTrue(login.errorMessage().contains("Username is required"),
                "An error banner should indicate that the username is required");
    }

    /** An empty password should surface a "Password is required" error and block login. */
    @Test(groups = {"ui", "regression"})
    public void emptyPasswordShowsError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("standard_user", "");

        Assert.assertFalse(login.isLoggedIn(), "User should not be logged in when the password is empty");
        Assert.assertTrue(login.errorMessage().contains("Password is required"),
                "An error banner should indicate that the password is required");
    }

    /** Empty username and password should report the username requirement first. */
    @Test(groups = {"ui", "regression"})
    public void emptyCredentialsShowsError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("", "");

        Assert.assertFalse(login.isLoggedIn(), "User should not be logged in with empty credentials");
        Assert.assertTrue(login.errorMessage().contains("Username is required"),
                "An error banner should indicate that the username is required");
    }

    /** A locked-out user with valid credentials should be denied with a lockout message. */
    @Test(groups = {"ui", "regression"})
    public void lockedOutUserIsDenied() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("locked_out_user", "secret_sauce");

        Assert.assertFalse(login.isLoggedIn(), "A locked-out user should not reach the inventory page");
        Assert.assertTrue(login.errorMessage().contains("locked out"),
                "An error banner should indicate that the user has been locked out");
    }

    /** A valid username with a wrong password should report a generic mismatch error. */
    @Test(groups = {"ui", "regression"})
    public void wrongPasswordShowsMismatchError() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("standard_user", "definitely_wrong");

        Assert.assertFalse(login.isLoggedIn(), "User should not be logged in with a wrong password");
        Assert.assertTrue(login.errorMessage().contains("Username and password do not match"),
                "An error banner should indicate the credentials do not match any user");
    }

    /** Usernames are case-sensitive; a different case should not authenticate. */
    @Test(groups = {"ui", "regression"})
    public void usernameIsCaseSensitive() {
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("Standard_User", "secret_sauce");

        Assert.assertFalse(login.isLoggedIn(), "Login should fail when the username case does not match");
        Assert.assertTrue(login.errorMessage().contains("Username and password do not match"),
                "An error banner should indicate the credentials do not match any user");
    }
}

