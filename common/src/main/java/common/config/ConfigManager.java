package common.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised, environment-aware configuration reader for the shared (UI + hybrid) layers.
 * <p>
 * <b>Design pattern:</b> Singleton (thread-safe, eager initialisation).
 * </p>
 *
 * <p>Resolution order (highest precedence first):</p>
 * <ol>
 *   <li>JVM system properties (e.g. {@code -Dbrowser=firefox})</li>
 *   <li>Environment-specific API file {@code config/api.<env>.properties}</li>
 *   <li>Environment-specific UI file {@code config/ui.<env>.properties}</li>
 *   <li>Shared API base config {@code apiBaseConfig.properties} (relocated from the API module)</li>
 * </ol>
 *
 * <p>Supported environments: {@code qa} (default) and {@code stage}. UI-wide defaults
 * (browser, waits, grid, retry) live in each {@code ui.<env>.properties} file.</p>
 *
 * <p>This class intentionally lives in {@code common} so both UI and hybrid modules
 * share one source of truth. {@code apiBaseConfig.properties} is kept at the classpath
 * root so the existing, untouched API module ({@code api.config.ConfigManager}) still
 * resolves it via {@code getResourceAsStream("apiBaseConfig.properties")}.</p>
 */
public final class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private final Properties properties = new Properties();
    private final String environment;

    private ConfigManager() {
        this.environment = System.getProperty("env", "qa").trim().toLowerCase();
        load("apiBaseConfig.properties");                  // shared API base config (classpath root)
        load("config/ui." + environment + ".properties");  // UI env config (incl. shared UI defaults)
        load("config/api." + environment + ".properties"); // API env config
    }

    /** @return the single shared instance. */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    private void load(String resource) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config resource: " + resource, e);
        }
    }

    /** @return the active environment name (qa, stage, prod...). */
    public String env() {
        return environment;
    }

    /**
     * Returns a property; system properties win over file-based values.
     * @param key property key
     * @return value or {@code null} when absent
     */
    public String get(String key) {
        return System.getProperty(key, properties.getProperty(key));
    }

    /**
     * Returns a property or a fallback default.
     * @param key property key
     * @param defaultValue value used when the key is missing
     * @return resolved value
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    /** Convenience boolean accessor. */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /** Convenience int accessor. */
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Integer.parseInt(value.trim());
    }
}

