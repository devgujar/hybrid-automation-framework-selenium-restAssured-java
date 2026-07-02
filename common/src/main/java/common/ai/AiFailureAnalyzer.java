package common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertPathBuilderException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates a concise, AI-authored root-cause summary for a failing test.
 * <p>
 * <b>Why it lives in {@code common}:</b> failure triage is a cross-cutting reporting concern
 * shared by UI, API and hybrid suites, exactly like {@link common.listeners.TestListener}
 * which drives it.
 * </p>
 *
 * <p><b>Provider:</b> any OpenAI-compatible <em>Chat Completions</em> endpoint
 * (OpenAI, Azure OpenAI, Groq, local Ollama/LM&nbsp;Studio, ...). Configure via
 * {@code ai.*} keys (see {@code config/ui.<env>.properties}). It is <b>disabled by
 * default</b> and fully <b>null-safe</b>: any misconfiguration, network error or
 * non-200 response returns {@code null} so a failing test is never masked by an
 * analyzer problem.</p>
 *
 * <p><b>Secrets:</b> a single {@code ai.api.key} property drives the API key.
 * Resolution order: the system property {@code -Dai.api.key} (used in CI, where the
 * value is injected from a secret store) takes precedence, otherwise the
 * {@code ai.api.key} value from {@code config/ui.<env>.properties} (convenient for
 * local runs where a developer pastes the key directly).</p>
 */
public final class AiFailureAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(AiFailureAnalyzer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiFailureAnalyzer() {}

    /** @return {@code true} when the feature is switched on via {@code ai.failure.analysis.enabled}. */
    public static boolean isEnabled() {
        return ConfigManager.getInstance().getBoolean("ai.failure.analysis.enabled", false);
    }

    /**
     * Produces a short failure summary for the given test.
     *
     * @param testName   qualified test method name
     * @param throwable  the failure cause (may be {@code null})
     * @return a plain-text AI summary, or {@code null} if disabled/misconfigured/unavailable
     */
    public static String summarize(String testName, Throwable throwable) {
        if (!isEnabled()) {
            return null;
        }
        ConfigManager cfg = ConfigManager.getInstance();

        String apiKey = resolveApiKey(cfg);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI failure analysis enabled but no API key resolved; skipping. "
                    + "Set -Dai.api.key (CI: from a secret) or ai.api.key in config/ui.<env>.properties.");
            return null;
        }

        String url = cfg.get("ai.api.url", "https://api.groq.com/openai/v1/chat/completions");
        String model = cfg.get("ai.model", "llama-3.3-70b-versatile");
        int timeout = cfg.getInt("ai.timeout.seconds", 30);
        int maxTrace = cfg.getInt("ai.max.stacktrace.chars", 4000);
        int maxSource = cfg.getInt("ai.max.source.chars", 6000);

        try {
            String prompt = buildPrompt(testName, throwable, maxTrace, maxSource);
            String body = buildRequestBody(model, prompt);

            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout));
            if (cfg.getBoolean("ai.tls.insecure", false)) {
                log.warn("ai.tls.insecure=true — TLS certificate validation is DISABLED for the "
                        + "AI endpoint. Use for local/POC only; never enable in shared or CI environments.");
                clientBuilder.sslContext(insecureSslContext());
            }
            HttpClient client = clientBuilder.build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                log.warn("AI failure analysis HTTP {}: {}", response.statusCode(),
                        truncate(response.body(), 500));
                return null;
            }
            return extractContent(response.body());
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            if (root instanceof CertPathBuilderException || e instanceof SSLHandshakeException) {
                log.warn("AI failure analysis TLS trust error (likely a corporate proxy CA missing "
                        + "from the JVM truststore). Import the CA into cacerts, set "
                        + "-Djavax.net.ssl.trustStore, or (POC only) set ai.tls.insecure=true. Cause: {}",
                        root.toString());
            } else {
                log.warn("AI failure analysis unavailable: {}", e.toString());
            }
            return null;
        }
    }

    /**
     * Builds an {@link SSLContext} that trusts all certificates. <b>POC/local use only</b> —
     * this disables man-in-the-middle protection and must never be enabled in shared or CI runs.
     * Gated behind {@code ai.tls.insecure=true}.
     */
    private static SSLContext insecureSslContext() throws Exception {
        TrustManager[] trustAll = { new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        } };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new java.security.SecureRandom());
        return context;
    }

    private static String resolveApiKey(ConfigManager cfg) {
        // 1) System property (used by CI: -Dai.api.key=${{ secrets.GROQ_API_KEY }}).
        String fromProp = System.getProperty("ai.api.key");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        // 2) Config file value (used locally: developer sets ai.api.key directly).
        return cfg.get("ai.api.key");
    }

    private static String buildPrompt(String testName, Throwable throwable, int maxTrace, int maxSource) {
        String stack = stackTraceToString(throwable);
        String source = readTestSource(testName, maxSource);

        StringBuilder prompt = new StringBuilder()
                .append("You are an expert test-automation triage assistant for a Selenium + REST Assured ")
                .append("Java/TestNG framework. A test has failed. In 4-6 sentences, give a concise ")
                .append("failure analysis: the most likely root cause, whether it looks like a product ")
                .append("bug, a flaky/environment issue, or a test-code problem, and one concrete next ")
                .append("step to investigate. Be specific and avoid restating the whole stack trace.\n\n")
                .append("Failing test: ").append(testName).append("\n\n")
                .append("Failure details:\n").append(truncate(stack, maxTrace));

        if (source != null && !source.isBlank()) {
            prompt.append("\n\nBelow is the current source of the failing test class from this ")
                    .append("repository. Read it, then AFTER the failure analysis add a section titled ")
                    .append("'Suggested fix' containing the corrected test code inside a single ```java ")
                    .append("code block. Only include the method(s) or lines you changed (with enough ")
                    .append("surrounding context to apply the fix), keep changes minimal, and briefly ")
                    .append("note any assumption. If the failure looks like a genuine product bug rather ")
                    .append("than a test defect, say so instead of forcing a test change.\n\n")
                    .append("Test source (").append(testName).append("):\n")
                    .append("```java\n").append(truncate(source, maxSource)).append("\n```");
        }
        return prompt.toString();
    }

    /**
     * Best-effort lookup of the failing test's {@code .java} source within the repository so the
     * model can propose a corrected test. Derives the top-level class from the qualified test name,
     * maps it to {@code src/test/java/<pkg>/<Class>.java} and searches the working directory (and its
     * parent, to cover Maven's per-module {@code basedir}). Fully null-safe: returns {@code null} on
     * any problem so failure analysis still proceeds without the source.
     */
    private static String readTestSource(String testName, int maxSource) {
        try {
            if (testName == null || testName.isBlank() || maxSource <= 0) {
                return null;
            }
            int lastDot = testName.lastIndexOf('.');
            if (lastDot < 0) {
                return null;
            }
            // Strip the trailing method name, and any nested-class suffix, to get the top-level class.
            String classFqn = testName.substring(0, lastDot);
            int dollar = classFqn.indexOf('$');
            if (dollar >= 0) {
                classFqn = classFqn.substring(0, dollar);
            }
            String relativePath = "src/test/java/" + classFqn.replace('.', '/') + ".java";

            Path cwd = Paths.get("").toAbsolutePath();
            List<Path> bases = new ArrayList<>();
            bases.add(cwd);
            if (cwd.getParent() != null) {
                bases.add(cwd.getParent());
            }
            for (Path base : bases) {
                Path found = findSource(base, relativePath);
                if (found != null) {
                    log.debug("AI failure analysis found test source: {}", found);
                    return Files.readString(found);
                }
            }
            log.debug("AI failure analysis could not locate source for {} (looked for {}).",
                    testName, relativePath);
        } catch (Exception e) {
            log.debug("AI failure analysis could not read test source for {}: {}", testName, e.toString());
        }
        return null;
    }

    private static Path findSource(Path base, String relativePath) throws IOException {
        if (base == null || !Files.isDirectory(base)) {
            return null;
        }
        String suffix = "/" + relativePath;
        try (Stream<Path> stream = Files.walk(base, 10)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().replace('\\', '/').endsWith(suffix))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static String buildRequestBody(String model, String prompt) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", "You provide short, actionable software test failure analyses and, "
                + "when given the test source, a minimal corrected test in a Java code block.");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        return root.toString();
    }

    private static String extractContent(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isTextual()) {
                String text = content.asText().trim();
                return text.isEmpty() ? null : text;
            }
        } catch (Exception e) {
            log.warn("Could not parse AI response: {}", e.toString());
        }
        return null;
    }

    private static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return "No throwable captured for this failure.";
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "\n... [truncated]";
    }
}
