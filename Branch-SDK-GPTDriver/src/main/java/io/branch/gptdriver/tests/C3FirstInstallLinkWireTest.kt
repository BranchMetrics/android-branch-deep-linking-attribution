package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import org.junit.Rule
import org.junit.Test

/**
 * C3 cold_firstInstall — a link arriving on a device with no prior install.
 *
 * CI needs no wipe for this: the emulator is created fresh per job, so every
 * run is already a first install. Measured in run 33446080403, where a run
 * with WIPE_FIRST=1 produced a capture identical to one without.
 *
 * The link is generated in-app through Espresso rather than extracted by the
 * AI driver, so this runs without MOBILEBOOST_API_KEY. That generation puts a
 * /v1/url on the wire ahead of the scenario's own traffic, which the contract
 * has to account for.
 */
class C3FirstInstallLinkWireTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun firstInstallWithLinkEmitsWirePayload() {
        val url = generateLink()
        deliver(url)
        settle()
    }

    /** Taps "Create Branch Link" and reads the field it writes into. */
    private fun generateLink(): String {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        Thread.sleep(LINK_MS)
        return io.branch.gptdriver.LinkFieldReader.read()
    }

    /**
     * Delivers through ACTION_VIEW, which is what Android hands an app when a
     * link is tapped. MainActivity.onCreate passes the intent data straight to
     * requestDeepLinkData, so this is the production path rather than a hook.
     */
    private fun deliver(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(InstrumentationRegistry.getInstrumentation().targetContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    private fun settle() {
        Thread.sleep(SETTLE_MS)
    }

    private companion object {
        const val LINK_MS = 8_000L
        const val SETTLE_MS = 12_000L
    }
}
