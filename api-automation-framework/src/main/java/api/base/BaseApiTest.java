package api.base;

import common.config.ConfigManager;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeMethod;

/**
 * BaseApiTest is the base class for all API test classes.
 * <p>
 * It sets up the RestAssured base URI and SSL configuration before each test method.
 * The base URI is loaded from the configuration property 'api.base.uri'.
 * SSL relaxed validation can be enabled via the 'api.ssl.relaxed' property (default: true).
 * </p>
 */
public class BaseApiTest {
    static ConfigManager config = ConfigManager.getInstance();

    /**
     * Sets up the RestAssured base URI and SSL configuration before each test method.
     * The base URI is fetched from the configuration manager.
     * SSL relaxed validation is enabled if 'api.ssl.relaxed' property is true (default: true).
     */
    @BeforeMethod
    public void setUpBaseURI() {

        RestAssured.baseURI = config.get("api.base.uri");

        if (config.getBoolean("api.ssl.relaxed", true)) {
            RestAssured.useRelaxedHTTPSValidation();
        }
    }

}
