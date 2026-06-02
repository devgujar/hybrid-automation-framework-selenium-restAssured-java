package common.base;

import api.base.BaseApiTest;
import org.testng.annotations.BeforeClass;
import ui.base.BaseUiTest;

/**
 * Single hybrid base: inherits the full UI lifecycle from {@link BaseUiTest}
 * and reuses the unmodified API setup from {@link BaseApiTest} via composition.
 */
public abstract class BaseHybridTest extends BaseUiTest {

    private final BaseApiTest apiBase = new BaseApiTest();   // reuse API module as-is

    @BeforeClass(alwaysRun = true)
    public void setUpApiLayer() {
        apiBase.setUpBaseURI();   // RestAssured baseURI + relaxed SSL
    }
}
