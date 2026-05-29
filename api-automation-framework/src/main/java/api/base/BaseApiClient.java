package api.base;

import api.core.SpecFactory;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import java.util.Map;

/**
 * BaseApiClient provides generic methods to send HTTP requests using RestAssured.
 * Supports headers, query parameters, and payloads for flexible API testing.
 * All requests use the base specification from SpecFactory.
 */
public class BaseApiClient {

    /**
     * Sends an HTTP request with the specified method, path, payload, headers, and query parameters.
     *
     * @param method      HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param path        API endpoint path (relative to base URI)
     * @param payload     Request body as JSON string (optional)
     * @param headers     Map of request headers (optional)
     * @param queryParams Map of query parameters (optional)
     * @return Response object containing the API response
     */
    public static Response sendRequest(String method, String path, String payload,
                                       Map<String, String> headers,
                                       Map<String, String> queryParams) {

        RequestSpecification request = given();

        request.spec(SpecFactory.request());

        if (headers != null) {
            request.headers(headers);
        }

        if (queryParams != null) {
            request.queryParams(queryParams);
        }

        if (payload != null) {
            request.header("Content-Type", "application/json");
            request.body(payload);
        }

        if (method.equalsIgnoreCase("GET")) {
            request.header("Accept", "application/json");
        }

        return request
                .when()
                .request(method, path)
                .then()
                .extract().response();
    }

    /**
     * Sends an HTTP request with only method and path.
     *
     * @param method HTTP method
     * @param path   API endpoint path
     * @return Response object
     */
    public static Response sendRequest(String method, String path) {
        return sendRequest(method, path, null, null, null);
    }

    /**
     * Sends an HTTP request with method, path, and payload.
     *
     * @param method  HTTP method
     * @param path    API endpoint path
     * @param payload Request body as JSON string
     * @return Response object
     */
    public static Response sendRequest(String method, String path, String payload) {
        return sendRequest(method, path, payload, null, null);
    }

    /**
     * Sends an HTTP request with method, path, and query parameters.
     *
     * @param method      HTTP method
     * @param path        API endpoint path
     * @param queryParams Map of query parameters
     * @return Response object
     */
    public static Response sendRequest(String method, String path, Map<String, String> queryParams) {
        return sendRequest(method, path, null, null, queryParams);
    }
}
