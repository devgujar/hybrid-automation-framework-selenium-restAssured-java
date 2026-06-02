package common.base;

import api.base.BaseTest;
import org.testng.annotations.BeforeClass;
import ui.base.BaseUiTest;

/**
 * Single hybrid base: inherits the full UI lifecycle from {@link BaseUiTest}
 * and reuses the unmodified API setup from {@link BaseTest} via composition.
 */
public abstract class BaseHybridTest extends BaseUiTest {

    private final BaseTest apiBase = new BaseTest();   // reuse API module as-is

    @BeforeClass(alwaysRun = true)
    public void setUpApiLayer() {
        apiBase.setUpBaseURI();   // RestAssured baseURI + relaxed SSL
    }
}
