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

/**
 * The steps W1WarmHttpsWireTest and W2WarmUriSchemeWireTest share: launch the TestBed, generate
 * a link, send the app to the background, deliver a URI through startActivity, wait.
 *
 * The scenario is never closed here, and the drivers use no ActivityScenarioRule, for the same
 * reason: once a new intent has arrived through startActivity the scenario has lost lifecycle
 * control, so close() throws "Activity never becomes DESTROYED". DeepLinkWarmOpenHybridTest hit
 * the same wall. The runner cleans the activity up.
 */
internal class WarmScenarioDriver {

    private var scenario: ActivityScenario<MainActivity>? = null

    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
    }

    /**
     * Clicks Generate Link, then polls the short-URL field until it holds an https link, and
     * returns it. Fails at once if the TestBed reports a creation error, and after
     * [LINK_TIMEOUT_MS] if nothing arrives, so a failed link step is named as one instead of
     * surfacing later as an intent for a URL that is not one. Same check as
     * ScenarioLinkGenerator makes on the cold path.
     */
    fun generateLink(): String {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        val deadline = System.currentTimeMillis() + LINK_TIMEOUT_MS
        var field = LinkFieldReader.read()
        while (!field.startsWith("https://")) {
            check(!field.startsWith("ERROR:")) { "Link generation failed: '$field'" }
            check(System.currentTimeMillis() < deadline) {
                "No link within ${LINK_TIMEOUT_MS}ms, the field holds '$field'"
            }
            Thread.sleep(POLL_MS)
            field = LinkFieldReader.read()
        }
        return field
    }

    fun background() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
    }

    fun deliver(uri: String) {
        // setPackage, so the system still resolves the intent against the manifest. Naming
        // the component would work too and would skip resolution, but then a manifest that
        // no longer declares the link's host or scheme would not break the driver. It did
        // stop declaring the https host, unnoticed for a week, which is the argument for
        // keeping resolution in the path.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    fun settleShort() = Thread.sleep(SETTLE_SHORT_MS)

    fun settle() = Thread.sleep(SETTLE_MS)

    private companion object {
        const val LINK_TIMEOUT_MS = 30_000L
        const val POLL_MS = 250L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
