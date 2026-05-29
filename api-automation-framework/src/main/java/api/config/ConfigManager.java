package api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager is a utility class for loading and accessing configuration properties.
 * <p>
 * Loads properties from the 'baseConfig.properties' file located in the resources directory.
 * Provides static methods to retrieve property values with or without default values.
 * </p>
 */
public class ConfigManager {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("apiBaseConfig.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                throw new RuntimeException("baseConfig.properties not found in resources");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load baseConfig.properties", e);
        }
    }

    /**
     * Retrieves the property value for the given key.
     * @param key Property key
     * @return Property value, or null if not found
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Retrieves the property value for the given key.
     * @param key Property key
     * @return Property value, or null if not found
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Retrieves the property value for the given key, or returns the default value if not found.
     * @param key Property key
     * @param defaultValue Default value to return if property is not found
     * @return Property value or defaultValue
     */
    public static String get(String key, String defaultValue) {
        return get(key)==null ? defaultValue : get(key);
    }
}
