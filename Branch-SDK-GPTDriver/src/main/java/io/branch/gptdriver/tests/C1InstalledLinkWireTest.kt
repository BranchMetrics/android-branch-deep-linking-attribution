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
import io.branch.gptdriver.LinkFieldReader
import org.junit.Rule
import org.junit.Test

/**
 * C1 cold_https — a link arriving on a device that already has the app.
 *
 * This is the case that needs work on this platform, not C3. The emulator is
 * created fresh per job, so every run is a first install unless something
 * establishes one first. Measured in run 33446080403: a run with WIPE_FIRST=1
 * produced a capture identical to one without, because there was nothing to
 * wipe.
 *
 * So the install is established here: the rule's launch, plus link generation,
 * is the returning device's history. The link launch that follows is the
 * scenario. Same shape the iOS driver arrived at, for the opposite reason —
 * there the harness always uninstalls, here it never does.
 *
 * Runs without MOBILEBOOST_API_KEY: the link comes from the TestBed's own
 * button, read through Espresso rather than extracted by the AI driver.
 */
class C1InstalledLinkWireTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun linkOnInstalledDeviceEmitsWirePayload() {
        // The rule's launch and this generation are what make the device a
        // returning one by the time the link arrives.
        val url = generateLink()
        settleShort()

        deliver(url)
        settle()
    }

    private fun generateLink(): String {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        Thread.sleep(LINK_MS)
        return LinkFieldReader.read()
    }

    private fun deliver(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(InstrumentationRegistry.getInstrumentation().targetContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    private fun settleShort() = Thread.sleep(SETTLE_SHORT_MS)

    private fun settle() = Thread.sleep(SETTLE_MS)

    private companion object {
        const val LINK_MS = 8_000L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
