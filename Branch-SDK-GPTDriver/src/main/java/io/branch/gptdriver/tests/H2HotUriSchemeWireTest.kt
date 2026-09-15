package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * H2 hot_uriScheme - W2WarmUriSchemeWireTest's shape minus its background() call, so the
 * activity never leaves RESUMED and the scheme link lands in onNewIntent hot rather than warm.
 *
 * Sets branchlogs.txt aside once the bare launch has settled, before delivering the scheme link,
 * per the capture convention: the contract asserts only this scenario's own pair, not the bare
 * launch's.
 *
 * No ActivityScenarioRule, for the reason W1/W2 document: the rule's after() closes a scenario
 * that has lost lifecycle control once a new intent arrived through startActivity.
 *
 * Produces no assertion of its own. The capture is the output.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class H2HotUriSchemeWireTest {

    private var scenario: ActivityScenario<MainActivity>? = null

    /** adb install -r keeps app data, so a leftover capture from an earlier run would satisfy
     * clearCapturedLog's pre-condition without this run's launch having written anything. */
    @Before
    fun deleteAnyLeftoverCaptures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, LOG_FILE_NAME).delete()
        File(context.filesDir, PRECLEAR_FILE_NAME).delete()
    }

    @Test
    fun hotUriSchemeLinkEmitsWirePayload() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        settleShort()

        generateLink()
        settleShort()

        clearCapturedLog()

        deliver(SCHEME_URI)
        settle()
    }

    /** Not read back: this exists so the device is a returning one, as in W1/W2. */
    private fun generateLink() {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())
        Thread.sleep(LINK_MS)
    }

    /** Moves the bare launch's own traffic to a side file, so the contract measures only this
     * scenario's delivery while the pre-clear capture stays readable for diagnosis. */
    private fun clearCapturedLog() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val liveLog = File(context.filesDir, LOG_FILE_NAME)
        assertTrue(
            "bare launch wrote nothing to $LOG_FILE_NAME before the clear",
            liveLog.exists() && liveLog.length() > 0
        )
        if (!liveLog.renameTo(File(context.filesDir, PRECLEAR_FILE_NAME))) {
            fail("could not rename $LOG_FILE_NAME to $PRECLEAR_FILE_NAME; the clear did not happen")
        }
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
        const val LOG_FILE_NAME = "branchlogs.txt"
        const val PRECLEAR_FILE_NAME = "branchlogs.preclear.txt"
        const val LINK_MS = 8_000L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
