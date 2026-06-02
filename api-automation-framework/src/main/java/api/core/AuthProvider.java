package api.core;

import common.config.ConfigManager;
import io.restassured.specification.RequestSpecification;

/**
 * Centralised authentication strategy applier for API requests.
 * <p>
 * Supported types (config key: auth.type):
 * <ul>
 *   <li>none</li>
 *   <li>basic    - uses auth.username / auth.password</li>
 *   <li>bearer   - uses auth.token</li>
 *   <li>oauth2   - uses auth.oauth.token (mock/real)</li>
 *   <li>apiKey   - uses auth.header.name / auth.header.value</li>
 * </ul>
 * </p>
 */
public final class AuthProvider {

    private AuthProvider() {}

    static ConfigManager config = ConfigManager.getInstance();


    /**
     * Supported authentication types for API requests.
     */
    public enum AuthType { NONE, BASIC, BEARER, OAUTH2, API_KEY }

    /**
     * Applies the configured authentication strategy to the given RestAssured RequestSpecification.
     * <p>
     * The authentication type is determined by the 'auth.type' property in configuration.
     * </p>
     * @param spec RestAssured RequestSpecification to apply authentication to
     * @return RequestSpecification with authentication applied
     */
    public static RequestSpecification applyAuthentication(RequestSpecification spec) {

        AuthType type = resolveType();

        return switch (type) {
            case BASIC   -> spec.auth().preemptive().basic(
                    config.get("api.auth.username"),
                    config.get("api.auth.password"));
            case BEARER  -> spec.header("Authorization", "Bearer " + config.get("api.auth.token"));
            case OAUTH2  -> spec.auth().oauth2(config.get("api.auth.oauth.token"));
            case API_KEY -> spec.header(config.get("api.auth.header.name"),
                    config.get("api.auth.header.value"));
            case NONE    -> spec;
        };
    }

    /**
     * Resolves the authentication type from configuration.
     * <p>
     * Reads the 'auth.type' property, normalizes it, and returns the corresponding AuthType enum value.
     * Defaults to NONE if not set or invalid.
     * </p>
     * @return AuthType enum value
     */
    private static AuthType resolveType() {
        String authType = config.get("api.auth.type", "NONE")
                .trim().toUpperCase().replace('-', '_');
        try { return AuthType.valueOf(authType); }
        catch (IllegalArgumentException e) { return AuthType.NONE; }
    }
}
