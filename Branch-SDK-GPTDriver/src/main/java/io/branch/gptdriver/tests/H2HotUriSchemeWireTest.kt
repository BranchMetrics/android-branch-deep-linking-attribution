package io.branch.gptdriver.tests

import org.junit.Test

/**
 * hot_uriScheme: a scheme link delivered through startActivity while MainActivity is RESUMED.
 * W2WarmUriSchemeWireTest's delivery without its background() step, so the activity never
 * leaves RESUMED and the link lands in onNewIntent hot rather than warm.
 *
 * Needs a device that already holds a token, from cold_https running first; this line does not
 * wipe app data. The shared driver checks it on the launch open and fails by name without it.
 * The hot_uriScheme contract itself has no token rule. W1WarmHttpsWireTest explains why the
 * token comes from cold_https and not from this launch. The link generation step W2 has is
 * left out: it only added a /v1/url that this scenario sets aside anyway.
 *
 * The contract counts one deeplink and one open, the delivery's own, so the launch's pair is
 * set aside once the launch has settled (WireScenarioDriver.setCaptureAside), before the scheme
 * link is delivered. That moves the capture to branchlogs.preclear.txt, where it stays
 * readable for diagnosis, and fails by name unless the capture held the launch's deeplink and
 * open at that point. The delivery's counts are then taken from the empty file.
 *
 * No ActivityScenarioRule, for the reason WireScenarioDriver documents. The driver asserts that
 * MainActivity never stopped or died between the launch and the end of the delivery. The
 * capture is the output.
 *
 * Not in the L1 workflow yet: run it by hand after cold_https with
 * TEST_CLASS=H2HotUriSchemeWireTest OUTPUT_LOG=wire-hot_uriScheme.txt CLEAR_LOG=1
 * ./scripts/run_l1_instrumented.sh, then validate_l1_logs.py --scenario hot_uriScheme.
 */
class H2HotUriSchemeWireTest {

    private val driver = WireScenarioDriver()

    @Test
    fun hotUriSchemeLinkEmitsWirePayload() {
        driver.launch()
        driver.setCaptureAside()
        driver.deliverHot(SCHEME_URI)
    }

    private companion object {
        const val SCHEME_URI = "branchtest://open"
    }
}
