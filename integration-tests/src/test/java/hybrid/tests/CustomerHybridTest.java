package hybrid.tests;

import api.clients.CustomerApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import common.base.BaseHybridTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import ui.base.BaseUiTest;
import ui.pages.LoginPage;

/**
 * Hybrid (UI + API) scenarios orchestrated WITHOUT modifying the API module.
 * <p>
 * It extends {@link BaseHybridTest} to inherit the thread-safe WebDriver lifecycle, the shared
 * {@code common.listeners.TestListener} and the RestAssured bootstrap, and uses the existing
 * {@link CustomerApiClient} (the unmodified REST Assured client) directly for the API side.
 * </p>
 *
 * <p>Patterns demonstrated:</p>
 * <ul>
 *   <li><b>Create data via API → validate via API</b> (fast, deterministic setup + cross-layer check)</li>
 *   <li><b>Fetch data via API → feed UI validation</b> (API output drives UI assertions)</li>
 * </ul>
 */
public class CustomerHybridTest extends BaseHybridTest {

    private final CustomerApiClient customer = new CustomerApiClient();
    private static final String CUSTOMER_ID = "1000";



    /**
     * Hybrid flow: set up a customer through the API layer, confirm it through the API,
     * then exercise the UI layer — all in one orchestrated test using both stacks.
     */
    @Test(groups = {"hybrid", "smoke"})
    public void createViaApi_thenValidate() throws JsonProcessingException {
        // --- API: arrange test data using the existing (unmodified) framework ---
        String payload = customer.getCustomerPayload(
                CUSTOMER_ID ,
                "testUser1000",
                "testUser1000@testUser100.com",
                "India","Pune","411001");
        customer.createCustomerIfNotExists(payload, CUSTOMER_ID);

        // --- API: cross-layer assertion that data exists ---
        Assert.assertTrue(customer.isCustomerRecordExists(CUSTOMER_ID),
                "Customer created via API should exist");
        Assert.assertEquals(fetchName(CUSTOMER_ID), "testUser1000",
                "API should report the name we created");

        // --- UI: drive the browser; in a real app this page would render the same entity ---
        LoginPage login = new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("standard_user", "secret_sauce");
        Assert.assertTrue(login.isLoggedIn(), "UI session should be established for the hybrid flow");
    }

    /**
     * Hybrid flow: API supplies the data the UI assertion is built from.
     */
    @Test(groups = {"hybrid", "regression"})
    public void apiDataFeedsUiValidation() throws JsonProcessingException {
        String basePayload = customer.getDefaultCustomerPayload();
        customer.createCustomerIfNotExists(basePayload, CUSTOMER_ID);

        String expectedName = fetchName(CUSTOMER_ID); // API is the source of truth
        Assert.assertNotNull(expectedName, "Precondition: API must return the customer name");

        new LoginPage(getDriver())
                .openAt(baseUrl())
                .login("standard_user", "secret_sauce");

        // Illustrative: a real UI page would expose the customer; we assert the API-derived value is usable.
        Assert.assertFalse(expectedName.isBlank(), "API-provided data should be usable by UI assertions");
    }

    /**
     * Fetches the customer name through the API.
     * @param id the customer id
     * @return the customer name as reported by the API, or {@code null} if absent.
     */
    private String fetchName(String id) {
        Response response = customer.getCustomer(id);
        return response.getStatusCode() == 200 ? response.jsonPath().getString("name") : null;
    }

    /** Tears down the test customer via the API so runs stay idempotent (no UI/API module changes). */
    @AfterClass(alwaysRun = true)
    public void cleanupData() {
        customer.deleteCustomerIfExists(CUSTOMER_ID); // tidy data via API, no UI/API module changes
    }
}

