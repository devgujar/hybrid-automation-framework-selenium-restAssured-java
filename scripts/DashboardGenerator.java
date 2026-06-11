import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated test dashboard generator (zero external dependencies).
 *
 * <p>Run with the JDK 17 single-file source launcher from the repo root:
 * <pre>
 *   java scripts/DashboardGenerator.java [repoRoot] [outputDir]
 * </pre>
 * Defaults: repoRoot = "." , outputDir = "docs/reports" (so the main docs/index.html can link it).</p>
 *
 * <p>For each module it parses {@code target/surefire-reports/testng-results.xml},
 * tallies real test methods (excluding TestNG config methods) by status, copies the
 * module's {@code target/extent-report.html} into the output folder, and writes a
 * single stylish {@code index.html} with clickable cards linking to each detailed report.</p>
 */
public class DashboardGenerator {

    /** One row in the dashboard. */
    static final class Module {
        final String key;          // stable id used for the copied report file name
        final String display;      // shown on the card
        final String surefireDir;  // relative to repoRoot
        final String extentReport; // relative to repoRoot
        int passed, failed, skipped;
        boolean resultsFound;
        String reportLink;         // populated if the extent report was copied

        Module(String key, String display, String surefireDir, String extentReport) {
            this.key = key;
            this.display = display;
            this.surefireDir = surefireDir;
            this.extentReport = extentReport;
        }
        int total() { return passed + failed + skipped; }
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path outputDir = repoRoot.resolve(args.length > 1 ? args[1] : "docs/reports");
        Files.createDirectories(outputDir);

        List<Module> modules = new ArrayList<>();
        modules.add(new Module("api", "API",
                "api-automation-framework/target/surefire-reports/testng-results.xml",
                "api-automation-framework/target/extent-report.html"));
        modules.add(new Module("ui", "UI",
                "ui-automation-framework/target/surefire-reports/testng-results.xml",
                "ui-automation-framework/target/extent-report.html"));
        modules.add(new Module("integration", "INTEGRATION",
                "integration-tests/target/surefire-reports/testng-results.xml",
                "integration-tests/target/extent-report.html"));

        for (Module m : modules) {
            parseResults(repoRoot.resolve(m.surefireDir), m);
            copyReport(repoRoot.resolve(m.extentReport), outputDir, m);
        }

        Path index = outputDir.resolve("index.html");
        Files.writeString(index, renderHtml(modules));
        System.out.println("Dashboard written to: " + index);
        for (Module m : modules) {
            System.out.printf("  %-12s %d/%d passed (failed=%d, skipped=%d)%s%n",
                    m.display, m.passed, m.total(), m.failed, m.skipped,
                    m.resultsFound ? "" : "  [no results found]");
        }
    }

    /** Counts non-config test methods by status from a TestNG results XML. */
    static void parseResults(Path xml, Module m) {
        if (!Files.exists(xml)) {
            return;
        }
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(xml.toFile());
            NodeList methods = doc.getElementsByTagName("test-method");
            for (int i = 0; i < methods.getLength(); i++) {
                Node n = methods.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                Element e = (Element) n;
                if ("true".equalsIgnoreCase(e.getAttribute("is-config"))) {
                    continue; // skip @BeforeX/@AfterX config methods
                }
                String status = e.getAttribute("status");
                switch (status == null ? "" : status.toUpperCase()) {
                    case "PASS" -> m.passed++;
                    case "FAIL" -> m.failed++;
                    case "SKIP" -> m.skipped++;
                    default -> { /* ignore unknown */ }
                }
            }
            m.resultsFound = true;
        } catch (Exception ex) {
            System.err.println("WARN: could not parse " + xml + " : " + ex.getMessage());
        }
    }

    /** Copies a module's extent report into the output dir as &lt;key&gt;-report.html. */
    static void copyReport(Path report, Path outputDir, Module m) {
        if (!Files.exists(report)) {
            return;
        }
        try {
            String fileName = m.key + "-report.html";
            Files.copy(report, outputDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            m.reportLink = fileName;
        } catch (IOException ex) {
            System.err.println("WARN: could not copy " + report + " : " + ex.getMessage());
        }
    }

    /** CSS kept as a raw constant so its many '%' characters never hit String formatting. */
    static final String CSS = """
          :root{
            --bg:#0f172a; --bg2:#1e293b; --card:#1e293b; --text:#e2e8f0; --muted:#94a3b8;
            --pass:#22c55e; --fail:#ef4444; --skip:#f59e0b; --accent:#6366f1;
          }
          *{box-sizing:border-box;margin:0;padding:0}
          body{font-family:'Segoe UI',system-ui,-apple-system,sans-serif;
               background:linear-gradient(135deg,#0f172a 0%,#1e1b4b 100%);
               color:var(--text);min-height:100vh;padding:40px 20px}
          .wrap{max-width:1100px;margin:0 auto}
          header{text-align:center;margin-bottom:40px}
          header h1{font-size:2rem;font-weight:700;letter-spacing:.5px}
          header h1 .spark{background:linear-gradient(90deg,#818cf8,#22d3ee);
               -webkit-background-clip:text;background-clip:text;color:transparent}
          header p{color:var(--muted);margin-top:8px;font-size:.9rem}
          .overall{display:flex;align-items:center;justify-content:center;gap:30px;
               background:rgba(30,41,59,.6);border:1px solid rgba(148,163,184,.15);
               border-radius:16px;padding:24px 30px;margin-bottom:36px;flex-wrap:wrap}
          .ring{width:120px;height:120px;border-radius:50%;
               display:grid;place-items:center;position:relative}
          .ring::before{content:"";position:absolute;inset:12px;border-radius:50%;background:#0f172a}
          .ring span{position:relative;font-size:1.6rem;font-weight:700}
          .legend{display:flex;flex-direction:column;gap:8px}
          .legend div{display:flex;align-items:center;gap:10px;font-size:.95rem}
          .dot{width:12px;height:12px;border-radius:50%}
          .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:24px}
          .card{background:var(--card);border:1px solid rgba(148,163,184,.15);
               border-radius:16px;padding:24px;text-decoration:none;color:inherit;
               transition:transform .18s ease,box-shadow .18s ease,border-color .18s ease;
               display:block;position:relative;overflow:hidden}
          .card.clickable:hover{transform:translateY(-6px);
               box-shadow:0 18px 40px rgba(0,0,0,.45);border-color:var(--accent)}
          .card .badge{position:absolute;top:18px;right:18px;font-size:.7rem;
               text-transform:uppercase;letter-spacing:1px;color:var(--muted)}
          .card h2{font-size:1.25rem;margin-bottom:4px}
          .ratio{font-size:2.4rem;font-weight:800;margin:10px 0 4px}
          .ratio small{font-size:1rem;color:var(--muted);font-weight:500}
          .pct{font-size:.85rem;color:var(--muted);margin-bottom:14px}
          .bar{height:10px;border-radius:6px;background:rgba(148,163,184,.18);
               overflow:hidden;display:flex}
          .bar i{display:block;height:100%}
          .counts{display:flex;gap:16px;margin-top:14px;font-size:.85rem;flex-wrap:wrap}
          .counts span{display:flex;align-items:center;gap:6px;color:var(--muted)}
          .counts b{color:var(--text)}
          .cta{margin-top:16px;font-size:.85rem;color:#a5b4fc;font-weight:600}
          .cta.muted{color:var(--muted)}
          .back-btn{display:inline-flex;align-items:center;gap:8px;margin-top:18px;
               padding:10px 20px;border-radius:999px;font-size:.9rem;font-weight:600;
               color:#0f172a;text-decoration:none;border:1px solid transparent;
               background:linear-gradient(135deg,#818cf8,#22d3ee);
               transition:transform .15s ease,box-shadow .15s ease}
          .back-btn:hover{transform:translateY(-2px);box-shadow:0 10px 24px rgba(0,0,0,.4)}
          footer{text-align:center;color:var(--muted);margin-top:40px;font-size:.8rem}
        """;

    static String renderHtml(List<Module> modules) {
        int totPassed = 0, totFailed = 0, totSkipped = 0;
        for (Module m : modules) { totPassed += m.passed; totFailed += m.failed; totSkipped += m.skipped; }
        int grand = totPassed + totFailed + totSkipped;
        int overallPct = grand == 0 ? 0 : (int) Math.round(100.0 * totPassed / grand);
        String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder cards = new StringBuilder();
        for (Module m : modules) {
            cards.append(renderCard(m));
        }

        String ringStyle = "background:conic-gradient(var(--pass) " + overallPct
                + "%,rgba(148,163,184,.18) 0)";

        return "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n<head>\n"
            + "<meta charset=\"UTF-8\"/>\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n"
            + "<title>Hybrid Automation - Consolidated Dashboard</title>\n"
            + "<style>\n" + CSS + "</style>\n"
            + "</head>\n<body>\n<div class=\"wrap\">\n"
            + "  <header>\n"
            + "    <h1><span class=\"spark\">Hybrid Automation</span> &mdash; Consolidated Dashboard</h1>\n"
            + "    <p>Selenium + REST Assured &middot; UI &middot; API &middot; Integration</p>\n"
            + "    <a class=\"back-btn\" href=\"../index.html\">&larr; Back to project home</a>\n"
            + "  </header>\n"
            + "  <section class=\"overall\">\n"
            + "    <div class=\"ring\" style=\"" + ringStyle + "\"><span>" + overallPct + "%</span></div>\n"
            + "    <div class=\"legend\">\n"
            + "      <div><i class=\"dot\" style=\"background:var(--pass)\"></i>Passed&nbsp;<b>" + totPassed + "</b></div>\n"
            + "      <div><i class=\"dot\" style=\"background:var(--fail)\"></i>Failed&nbsp;<b>" + totFailed + "</b></div>\n"
            + "      <div><i class=\"dot\" style=\"background:var(--skip)\"></i>Skipped&nbsp;<b>" + totSkipped + "</b></div>\n"
            + "      <div><i class=\"dot\" style=\"background:#64748b\"></i>Total&nbsp;<b>" + grand + "</b></div>\n"
            + "    </div>\n"
            + "  </section>\n"
            + "  <section class=\"grid\">\n" + cards + "  </section>\n"
            + "  <footer>Generated " + generated
            + " &middot; click a card to open the module's detailed Extent report</footer>\n"
            + "</div>\n</body>\n</html>\n";
    }

    static String renderCard(Module m) {
        int total = m.total();
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * m.passed / total);
        String pPass = String.format("%.1f", total == 0 ? 0.0 : 100.0 * m.passed / total);
        String pFail = String.format("%.1f", total == 0 ? 0.0 : 100.0 * m.failed / total);
        String pSkip = String.format("%.1f", total == 0 ? 0.0 : 100.0 * m.skipped / total);

        boolean clickable = m.reportLink != null;
        String openTag = clickable
                ? "<a class=\"card clickable\" href=\"" + m.reportLink + "\">"
                : "<div class=\"card\">";
        String closeTag = clickable ? "</a>" : "</div>";
        String cta = clickable
                ? "<div class=\"cta\">View detailed report &rarr;</div>"
                : "<div class=\"cta muted\">No detailed report available</div>";
        String status = m.resultsFound ? "" : " &middot; no results";

        return openTag + "\n"
            + "  <div class=\"badge\">" + m.display + status + "</div>\n"
            + "  <h2>" + m.display + "</h2>\n"
            + "  <div class=\"ratio\">" + m.passed + "<small>/" + total + "</small></div>\n"
            + "  <div class=\"pct\">" + pct + "% passed</div>\n"
            + "  <div class=\"bar\">\n"
            + "    <i style=\"width:" + pPass + "%;background:var(--pass)\"></i>\n"
            + "    <i style=\"width:" + pFail + "%;background:var(--fail)\"></i>\n"
            + "    <i style=\"width:" + pSkip + "%;background:var(--skip)\"></i>\n"
            + "  </div>\n"
            + "  <div class=\"counts\">\n"
            + "    <span><i class=\"dot\" style=\"background:var(--pass)\"></i><b>" + m.passed + "</b> passed</span>\n"
            + "    <span><i class=\"dot\" style=\"background:var(--fail)\"></i><b>" + m.failed + "</b> failed</span>\n"
            + "    <span><i class=\"dot\" style=\"background:var(--skip)\"></i><b>" + m.skipped + "</b> skipped</span>\n"
            + "  </div>\n"
            + "  " + cta + "\n"
            + closeTag + "\n";
    }
}

