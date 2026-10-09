package io.branch.gptdriver.tests

import org.junit.Test

/**
 * hot_https_foreground: an https App Link delivered through startActivity while MainActivity is
 * RESUMED, so it lands in onNewIntent hot rather than warm. H2HotUriSchemeWireTest's delivery
 * with an App Link in place of the scheme link.
 *
 * Needs a device that already holds a token, from cold_https running first; this line does not
 * wipe app data. The shared driver checks it on the launch open and fails by name without it.
 * W1WarmHttpsWireTest explains why the token comes from cold_https and not from this launch.
 *
 * The link is generated in the running activity with the scenario marker the contract reads
 * back (WireScenarioDriver.generateMarkedLink). The contract counts one deeplink, one open and
 * no /v1/url, the delivery's own, so the launch's pair and the link generation are set aside
 * once they have settled (WireScenarioDriver.setCaptureAside), before the link is delivered.
 * The file stays readable for diagnosis through run-as only: the L1 script does not pull it.
 *
 * No ActivityScenarioRule, for the reason WireScenarioDriver documents. The driver asserts that
 * MainActivity is RESUMED before and after the delivery and never stopped or died between the
 * launch and its end, which is what separates hot from warm; the wire does not carry it. The
 * capture is the output.
 */
class H1HotAppLinkWireTest {

    private val driver = WireScenarioDriver()

    @Test
    fun hotAppLinkEmitsWirePayload() {
        driver.launch()
        val link = driver.generateMarkedLink(SCENARIO_NAME)
        driver.setCaptureAside()
        driver.deliverHot(link)
    }

    private companion object {
        const val SCENARIO_NAME = "hot_https_foreground"
    }
}
