package ui.support;

import org.openqa.selenium.By;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Central store + builder for reusable XPath templates.
 * <p>
 * Templates live in {@code xpaths/xpath.store.properties} (classpath) so locator
 * <i>shapes</i> are defined in one place and reused across all Page Objects. A single
 * {@link #buildXpath(String, String...)} function fills the template placeholders dynamically.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * // ELEMENT_BY_TAG_ID = //%s[contains(@%s,'%s')]
 * By login = XPathStore.by("ELEMENT_BY_TAG_ID", "input", "id", "login-button");
 * // -> //input[contains(@id,'login-button')]
 * }</pre>
 */
public final class XPathStore {

    private static final String RESOURCE = "xpaths/xpath.store.properties";
    private static final Properties TEMPLATES = load();

    private XPathStore() {}

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = XPathStore.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("XPath store not found on classpath: " + RESOURCE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load XPath store: " + RESOURCE, e);
        }
        return props;
    }

    /**
     * Builds an XPath string dynamically from a named template and its arguments.
     * <p>
     * The template and every argument are <b>trimmed of surrounding whitespace</b> before
     * formatting, so accidental spaces in the properties file or callers never leak into the
     * generated locator.
     * </p>
     * @param key  template key from {@code xpath.store.properties} (e.g. {@code ELEMENT_BY_TAG_ID})
     * @param args values that fill the template placeholders, in order
     * @return the formatted XPath string
     * @throws IllegalArgumentException when the key is unknown
     */
    public static String buildXpath(String key, String... args) {
        String template = TEMPLATES.getProperty(key);
        if (template == null) {
            throw new IllegalArgumentException("Unknown XPath template key: " + key);
        }
        Object[] trimmed = Arrays.stream(args)
                .map(a -> a == null ? "" : a.trim())
                .toArray();
        return String.format(template.trim(), trimmed);
    }

    /**
     * Convenience wrapper returning a Selenium {@link By} locator.
     * @param key  template key
     * @param args placeholder values, in order
     * @return {@link By#xpath(String)} built from {@link #buildXpath(String, String...)}
     */
    public static By by(String key, String... args) {
        return By.xpath(buildXpath(key, args));
    }
}

