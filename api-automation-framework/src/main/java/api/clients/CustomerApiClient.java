package api.clients;

import api.base.BaseApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomerApiClient provides methods to interact with the Customer API endpoints.
 * <p>
 * Supports operations such as get, create, update, and delete customer records.
 * Also provides utility methods for payload creation and conditional operations.
 * </p>
 */
public class CustomerApiClient extends BaseApiClient {

    String CUSTOMER_BASE_PATH = "/customer" ;

    /**
     * Retrieves a specific customer by customerId.
     * @param customerId Customer ID
     * @return Response object
     */
    public Response getCustomer( String customerId ){
        Response response = BaseApiClient.sendRequest("GET", CUSTOMER_BASE_PATH + "/"+ customerId);
        return response;
    }

    /**
     * Retrieves all customers.
     * @return Response object
     */
    public Response getAllCustomers(  ){
        Response response = BaseApiClient.sendRequest("GET", CUSTOMER_BASE_PATH);
        return response;
    }

    /**
     * Creates a new customer with the given payload.
     * @param basePayload JSON payload string
     * @return Response object
     */
    public Response createCustomer(String basePayload){
        Response response = BaseApiClient.sendRequest("POST", CUSTOMER_BASE_PATH, basePayload);
        return response;
    }

    /**
     * Deletes a customer by customerId.
     * @param customerId Customer ID
     * @return Response object
     */
    public Response deleteCustomer( String customerId ){
        Response response = BaseApiClient.sendRequest("DELETE", CUSTOMER_BASE_PATH + "/"+ customerId);
        return response;
    }

    /**
     * Updates a customer with the given payload.
     * @param basePayload JSON payload string
     * @return Response object
     */
    public Response updateCustomer( String basePayload ){
        Response response = BaseApiClient.sendRequest("PUT", CUSTOMER_BASE_PATH, basePayload);
        return response;
    }

    /**
     * Checks if a customer record exists for the given customerId.
     * @param customerId Customer ID
     * @return true if exists, false otherwise
     */
    public boolean isCustomerRecordExists( String customerId ){
        Response response = BaseApiClient.sendRequest("GET", CUSTOMER_BASE_PATH + "/"+ customerId);
        return response.getStatusCode() == 200 ? true : false;
    }

    /**
     * Deletes a customer if the record exists.
     * @param customerId Customer ID
     */
    public void deleteCustomerIfExists( String customerId ){
        if(isCustomerRecordExists(customerId)) {
            deleteCustomer(customerId);
        }
    }

    /**
     * Creates a customer if the record does not exist.
     * @param basePayload JSON payload string
     * @param customerId Customer ID
     */
    public void createCustomerIfNotExists( String basePayload , String customerId ){
        if(!isCustomerRecordExists(customerId)) {
            createCustomer(basePayload);
        }
    }

    /**
     * Builds a customer payload with required fields.
     * @param number Customer ID
     * @param testUser100 Customer name
     * @param mail Customer email
     * @return JSON payload string
     * @throws JsonProcessingException if serialization fails
     */
    public String getCustomerPayload(String number, String testUser100, String mail) throws JsonProcessingException {
        return getCustomerPayload(number,testUser100,mail,null,null,null );
    }

    /**
     * Builds a customer payload with all fields.
     * @param number Customer ID
     * @param testUser100 Customer name
     * @param mail Customer email
     * @param country Country
     * @param city City
     * @param pin_code Pin code
     * @return JSON payload string
     * @throws JsonProcessingException if serialization fails
     */
    public String getCustomerPayload(
            String number,
            String testUser100,
            String mail,
            String country,
            String city,
            String pin_code
    ) throws JsonProcessingException {

        Map<String, Object> address = new HashMap<>();
        address.put("country", country);
        address.put("city", city);
        address.put("pin_code", pin_code);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", number);
        payload.put("name", testUser100);
        payload.put("email", mail);
        payload.put("address", address);

        ObjectMapper mapper = new ObjectMapper();
        String basePayload = mapper.writeValueAsString(payload);
        return basePayload;
    }

    /**
     * Builds a default customer payload for testing.
     * @return JSON payload string
     * @throws JsonProcessingException if serialization fails
     */
    public String getDefaultCustomerPayload() throws JsonProcessingException {
        return getCustomerPayload(
                "1",
                "testUser1",
                "testUser1@testUser1.com",
                "India",
                "Pune",
                "411001");
    }

}
