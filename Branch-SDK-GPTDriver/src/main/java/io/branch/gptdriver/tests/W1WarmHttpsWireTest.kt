package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.LinkFieldReader
import org.junit.Test

/**
 * W1 warm_https_onNewIntent — a link arriving while the app is alive but backgrounded.
 *
 * Warm is defined by the launch state, not by the delivery. The app must actually be in the
 * background when the link arrives, which is why this presses home before delivering. The
 * existing DeepLinkWarmOpenHybridTest delivers with the app in the foreground and its own
 * header calls that the hot case, so it is a precedent for the mechanism and not for the
 * state.
 *
 * Delivery preserves the task. C1 uses FLAG_ACTIVITY_CLEAR_TASK, which tears it down and is
 * the cold shape; SINGLE_TOP lands in MainActivity.onNewIntent instead, which is the entry
 * point a warm open really uses. Going through startActivity rather than calling onNewIntent
 * directly is deliberate: it re-runs onActivityStarted and onActivityResumed, so the SDK's
 * PENDING -> READY intent transition happens the way it does in production.
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

    private var scenario: ActivityScenario<MainActivity>? = null

    @Test
    fun warmHttpsLinkEmitsWirePayload() {
        // This launch and the generation below happen first, so the device is a returning
        // one and the app is running by the time the link arrives.
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        settleShort()

        val url = generateLink()
        settleShort()

        background()
        settleShort()

        deliver(url)
        settle()
    }

    private fun generateLink(): String {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        Thread.sleep(LINK_MS)
        return LinkFieldReader.read()
    }

    private fun background() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
    }

    private fun deliver(url: String) {
        // setPackage, so the system still resolves the intent against the manifest.
        // Naming the component explicitly would work too and would skip resolution,
        // but then a manifest that no longer declares the generated link's host would
        // not break this test. It did stop declaring it, unnoticed for a week, which
        // is the argument for keeping resolution in the path.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    private fun settleShort() = Thread.sleep(SETTLE_SHORT_MS)

    private fun settle() = Thread.sleep(SETTLE_MS)

    private companion object {
        const val LINK_MS = 8_000L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
