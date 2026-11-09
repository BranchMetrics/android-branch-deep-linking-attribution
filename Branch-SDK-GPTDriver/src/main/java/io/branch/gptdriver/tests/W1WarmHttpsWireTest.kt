package io.branch.gptdriver.tests

import org.junit.After
import org.junit.Test

/**
 * warm_https_onNewIntent: a link arriving while the app is alive but backgrounded.
 *
 * Warm is defined by the launch state, not by the delivery. The app must actually be in the
 * background when the link arrives, which is why this presses Home and waits for the activity
 * to stop before delivering. DeepLinkWarmOpenHybridTest delivers with the app in the
 * foreground ("App is in foreground" in its header), so it is a precedent for the mechanism
 * and not for the state.
 *
 * Needs a device that already holds a token. The contract expects both opens to carry
 * randomized_bundle_token, and only the reply to an init-session request stores it
 * (BranchRequestQueue.processInitSessionResponse). Neither this driver's launch nor
 * generating a link does. The token comes from cold_https, which the L1 workflow runs first,
 * and this line does not wipe app data. Run alone on a wiped device, the first launch is an
 * install and the gate fails on the token count and the custom event count, which reads like
 * an SDK bug and is a missing prerequisite.
 *
 * Delivery preserves the task. cold_https is delivered from the host: scripts/
 * run_l1_instrumented.sh force-stops the app and runs `am start -W`, and fails unless the
 * launch is COLD. Here the process stays alive and FLAG_ACTIVITY_SINGLE_TOP lands the intent
 * in the running MainActivity.onNewIntent, the entry point a warm open uses. It goes through
 * startActivity rather than calling onNewIntent directly, so the system resolves the intent
 * against the manifest and brings the stopped activity back the way a tap would. The SDK's
 * BranchProcessLifecycleObserver.onStop does clear sessionParams and the saved launch link when
 * the app goes to the background, which is why the driver waits for the stop before delivering;
 * after that the SDK holds no launch intent state to re-run. The TestBed's onNewIntent calls
 * requestDeepLinkData, which sends the deeplink and the open.
 *
 * No ActivityScenarioRule here, unlike the other L1 drivers. The rule closes the scenario in
 * its after(), and once a new intent has been delivered through startActivity the scenario
 * has lost lifecycle control, so close() throws. DeepLinkWarmOpenHybridTest hit the same wall
 * and manages its own scenario for the same reason. The runner cleans the activity up.
 *
 * The driver checks only its own preconditions: the launch open carries a token, the activity
 * stopped without being destroyed, and it was not destroyed by the delivery. The capture is the
 * output; the contract that judges it lives in the validator.
 */
class W1WarmHttpsWireTest {

    private val driver = WireScenarioDriver()

    /** Stops this driver observing the Application, so the next test's counters are its own. */
    @After
    fun closeDriver() = driver.close()

    @Test
    fun warmHttpsLinkEmitsWirePayload() {
        // The launch and the link generation put the app in the running state a warm link
        // needs. They do not make the device a returning one: that is cold_https's token.
        driver.launch()
        val url = driver.generateLink()
        driver.background()
        driver.deliver(url)
    }
}
