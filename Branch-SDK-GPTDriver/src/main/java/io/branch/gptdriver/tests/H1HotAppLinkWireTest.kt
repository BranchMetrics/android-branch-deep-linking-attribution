package io.branch.gptdriver.tests

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * hot_https_foreground: an App Link delivered to an activity that never left RESUMED, so
 * resolution runs through onNewIntent rather than a cold launch path.
 *
 * Sets branchlogs.txt aside once the bare launch has settled, before delivering the App
 * Link, per the capture convention: the contract asserts only this scenario's own pair.
 *
 * No ActivityScenarioRule: its after() closes a scenario that has lost lifecycle control
 * once a new intent arrives through startActivity, the same reason the hot scheme driver
 * avoids it.
 *
 * Produces no assertion of its own beyond the generated link. The capture is the output.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class H1HotAppLinkWireTest {

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
    fun hotAppLinkEmitsWirePayload() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        settleShort()

        val stoppedWatcher = StoppedWatcher()
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        application.registerActivityLifecycleCallbacks(stoppedWatcher)
        try {
            val link = generateLink()
            settleShort()

            clearCapturedLog()

            deliver(link)
            settle()

            assertTrue(
                "MainActivity left the foreground during the scenario, so the delivery " +
                    "was warm rather than hot",
                !stoppedWatcher.stopped.get()
            )
        } finally {
            application.unregisterActivityLifecycleCallbacks(stoppedWatcher)
        }
    }

    /** Watches for onStop, not onPause: onPause fires on every hot redelivery to a resumed
     * singleTop activity, but a true hot delivery never reaches STOPPED. */
    private class StoppedWatcher : Application.ActivityLifecycleCallbacks {
        val stopped = AtomicBoolean(false)

        override fun onActivityStopped(activity: Activity) {
            if (activity is MainActivity) stopped.set(true)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    /** Generated and read back in-activity, since the delivered link is the resolved one. */
    private fun generateLink(): String {
        val runId = System.currentTimeMillis().toString()
        val latch = CountDownLatch(1)
        var url: String? = null
        var error: BranchError? = null
        val properties = LinkProperties()
            .addControlParameter(KEY_SCENARIO, SCENARIO_NAME)
            .addControlParameter(KEY_RUN_ID, runId)
        scenario?.onActivity { activity ->
            BranchUniversalObject()
                .setCanonicalIdentifier("l1/$SCENARIO_NAME/$runId")
                .generateShortUrl(activity, properties, { generated, failure ->
                    url = generated
                    error = failure
                    latch.countDown()
                }, false)
        }

        assertTrue("no link within ${LINK_MS}ms", latch.await(LINK_MS, TimeUnit.MILLISECONDS))
        assertTrue("link generation failed: ${error?.message}", error == null)
        val link = url.orEmpty()
        assertTrue("expected an https link, got '$link'", link.startsWith("https://"))
        return link
    }

    /** Moves everything captured before delivery (launch and link generation) to a side file, so
     * the contract measures only this scenario's delivery. The side file stays for diagnosis. */
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
        // setPackage, so the manifest's App Link filter still has to match. Naming the
        // component would skip that, and the point of driving the real entry point is that
        // a manifest which stops declaring the host breaks this test.
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
        const val SCENARIO_NAME = "hot_https_foreground"
        const val KEY_SCENARIO = "l1_scenario"
        const val KEY_RUN_ID = "l1_run_id"
        const val LOG_FILE_NAME = "branchlogs.txt"
        const val PRECLEAR_FILE_NAME = "branchlogs.preclear.txt"
        const val LINK_MS = 15_000L
        const val SETTLE_SHORT_MS = 6_000L
        const val SETTLE_MS = 12_000L
    }
}
