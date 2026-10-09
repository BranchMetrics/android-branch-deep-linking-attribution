package io.branch.gptdriver.tests

import org.junit.Test

/**
 * warm_https_onNewIntent: a link arriving while the app is alive but backgrounded.
 *
 * Warm is defined by the launch state, not by the delivery. The app must actually be in the
 * background when the link arrives, which is why this presses home before delivering. The
 * existing DeepLinkWarmOpenHybridTest delivers with the app in the foreground and its own
 * header calls that the hot case, so it is a precedent for the mechanism and not for the
 * state.
 *
 * Delivery preserves the task. cold_https uses FLAG_ACTIVITY_CLEAR_TASK, which tears it down
 * and is the cold shape; SINGLE_TOP lands in MainActivity.onNewIntent instead, which is the
 * entry point a warm open really uses. Going through startActivity rather than calling
 * onNewIntent directly is deliberate: it re-runs onActivityStarted and onActivityResumed,
 * so the SDK's PENDING -> READY intent transition happens the way it does in production.
 *
 * No ActivityScenarioRule here, unlike the other L1 drivers. The rule closes the scenario in
 * its after(), and once a new intent has been delivered through startActivity the scenario
 * has lost lifecycle control, so close() throws. DeepLinkWarmOpenHybridTest hit the same wall
 * and manages its own scenario for the same reason. The runner cleans the activity up.
 *
 * Produces no assertion of its own. The capture is the output; the contract that judges it
 * lives in the validator.
 */
class W1WarmHttpsWireTest {

    private val driver = WarmScenarioDriver()

    @Test
    fun warmHttpsLinkEmitsWirePayload() {
        // This launch and the generation below happen first, so the app is running by the
        // time the link arrives.
        driver.launch()
        driver.settleShort()

        val url = driver.generateLink()
        driver.settleShort()

        driver.background()
        driver.settleShort()

        driver.deliver(url)
        driver.settle()
    }
}
