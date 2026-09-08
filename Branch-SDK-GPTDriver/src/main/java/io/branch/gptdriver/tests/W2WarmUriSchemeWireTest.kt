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
import org.junit.Test

/**
 * W2 warm_uriScheme — W1's launch state, entered through the URI scheme instead of https.
 *
 * Same shape as W1WarmHttpsWireTest: launch, generate a link so the device is a returning
 * one, background, deliver, settle. Only the delivered URI differs.
 *
 * The scheme changes which field carries the URI. RequestDeepLink puts an http or https URI
 * in android_app_link_url and everything else in external_intent_uri, and lifts a
 * link_click_id query parameter into link_identifier when one is present. A bare
 * branchtest:// URI therefore reaches /v3/deeplink with external_intent_uri set and no
 * link_identifier, which is what this drives and what its contract records.
 *
 * The URI form follows ReferringUrlUtilityTests, which uses branchtest://home and
 * branchtest://?gclid=12345. The TestBed's button generates https links only, so there is no
 * scheme link with a real click id to deliver here.
 *
 * No ActivityScenarioRule, for the reason W1 documents: the rule's after() closes a scenario
 * that has lost lifecycle control once a new intent arrived through startActivity.
 *
 * Produces no assertion of its own. The capture is the output.
 */
class W2WarmUriSchemeWireTest {

    private var scenario: ActivityScenario<MainActivity>? = null

    @Test
    fun warmUriSchemeLinkEmitsWirePayload() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        settleShort()

        generateLink()
        settleShort()

        background()
        settleShort()

        deliver(SCHEME_URI)
        settle()
    }

    /** Not read back: this exists so the device is a returning one, as in W1. */
    private fun generateLink() {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        Thread.sleep(LINK_MS)
    }

    private fun background() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
    }

    private fun deliver(uri: String) {
        // setPackage, so the manifest's branchtest filter still has to match. Naming the
        // component would skip that, and the point of driving the real entry point is that
        // a manifest which stops declaring the scheme breaks this test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    private fun settleShort() = Thread.sleep(SETTLE_SHORT_MS)

    private fun settle() = Thread.sleep(SETTLE_MS)

    private companion object {
        const val SCHEME_URI = "branchtest://open"
        const val LINK_MS = 8_000L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
