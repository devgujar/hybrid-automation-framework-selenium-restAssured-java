package api.base;

import io.restassured.RestAssured;
import api.config.ConfigManager;
import org.testng.annotations.BeforeMethod;

/**
 * BaseTest is the base class for all API test classes.
 * <p>
 * It sets up the RestAssured base URI and SSL configuration before each test method.
 * The base URI is loaded from the configuration property 'base.uri'.
 * SSL relaxed validation can be enabled via the 'ssl.relaxed' property (default: true).
 * </p>
 */
public class BaseTest {

    /**
     * Sets up the RestAssured base URI and SSL configuration before each test method.
     * The base URI is fetched from the configuration manager.
     * SSL relaxed validation is enabled if 'ssl.relaxed' property is true (default: true).
     */
    @BeforeMethod
    public void setUpBaseURI() {

        RestAssured.baseURI = ConfigManager.getProperty("base.uri");

        if (Boolean.parseBoolean(ConfigManager.get("ssl.relaxed", "true"))) {
            RestAssured.useRelaxedHTTPSValidation();
        }
    }

}
