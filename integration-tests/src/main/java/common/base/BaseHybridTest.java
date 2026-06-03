package common.base;

import api.base.BaseApiTest;
import org.testng.annotations.BeforeClass;
import ui.base.BaseUiTest;

/**
 * Single base class for hybrid (UI + API) tests.
 * <p>
 * <b>Why composition, not multiple inheritance:</b> Java allows only one superclass, so this
 * class {@code extends} {@link BaseUiTest} (inheriting the full thread-safe WebDriver lifecycle
 * and the shared listeners) and <i>composes</i> {@link BaseApiTest} to reuse the unmodified API
 * setup. {@link BaseApiTest#setUpBaseURI()} only sets static RestAssured state (base URI + relaxed
 * SSL), so invoking it on a held instance fully reproduces the API bootstrap without touching the
 * API module.
 * </p>
 * <p>Subclasses get UI lifecycle + RestAssured bootstrap + shared reporting/listeners for free.</p>
 */
public abstract class BaseHybridTest extends BaseUiTest {

    private final BaseApiTest apiBase = new BaseApiTest();   // reuse API module as-is

    /** Bootstraps the RestAssured base URI / SSL once per class via the unmodified API base. */
    @BeforeClass(alwaysRun = true)
    public void setUpApiLayer() {
        apiBase.setUpBaseURI();   // RestAssured baseURI + relaxed SSL
    }
}
