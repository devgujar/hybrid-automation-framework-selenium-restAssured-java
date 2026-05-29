package api.core;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.config.ConfigManager;

/**
 * SpecFactory provides centralized builders for RestAssured Request and Response specifications.
 * <p>
 * Handles base URI, base path, content type, timeouts, authentication, and conditional logging filters.
 * All API requests should use the request() method to ensure consistent configuration.
 * </p>
 */
public final class SpecFactory {

    private SpecFactory() {}

    /**
     * Builds and returns a configured RequestSpecification for API requests.
     * <p>
     * Applies base URI, base path, content type, timeouts, authentication, and logging filters (if enabled).
     * Logging is controlled by the 'enable.logging' property in configuration.
     * </p>
     * @return Configured RequestSpecification
     */
    public static RequestSpecification request() {
        int connTimeout = Integer.parseInt(ConfigManager.get("http.connect.timeout.ms", "10000"));
        int readTimeout = Integer.parseInt(ConfigManager.get("http.read.timeout.ms", "30000"));

        RestAssuredConfig cfg = RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", connTimeout)
                        .setParam("http.socket.timeout", readTimeout));

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.get("base.uri"))
                .setBasePath(ConfigManager.get("base.path", ""))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(cfg);

        // Conditionally add logging filters based on property
        boolean enableLogging = Boolean.parseBoolean(ConfigManager.get("enable.logging", "false"));
        if (enableLogging) {
            builder.addFilter(new RequestLoggingFilter());
            builder.addFilter(new ResponseLoggingFilter());
        }

        RequestSpecification spec = builder.build();
        return AuthProvider.applyAuthentication(spec);
    }

    /**
     * Returns a ResponseSpecification expecting HTTP 200 and JSON content type.
     * @return ResponseSpecification for successful JSON responses
     */
    public static ResponseSpecification responseOk() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}
