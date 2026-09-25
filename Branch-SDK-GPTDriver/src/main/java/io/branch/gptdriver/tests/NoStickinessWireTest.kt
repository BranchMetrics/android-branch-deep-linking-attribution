package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import io.branch.referral.PrefHelper
import io.branch.referral.util.LinkProperties
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Test

/**
 * no_stickiness: after a link resolves and its chained open completes, a return that does not
 * itself resolve a link must not leave the prior link behind. This class covers delivery and
 * the chained open only; background, return and the verdict are added separately.
 *
 * Manages its own [ActivityScenario] instead of an [androidx.test.ext.junit.rules.ActivityScenarioRule]:
 * delivering a new intent via `startActivity` leaves the scenario unable to reach DESTROYED on
 * close, which the rule's teardown requires.
 */
class NoStickinessWireTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val captureFile = File(context.filesDir, "branchlogs.txt")
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        // Do not close(): after the warm-delivery startActivity call, the scenario can no
        // longer be driven to DESTROYED and close() throws. The runner reclaims the process.
    }

    @Test
    fun noStickinessAfterReturn() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val runId = System.currentTimeMillis().toString()
        Log.i(
            TAG,
            "apk_testbed=${apkIdentifier(context.packageName)} " +
                "apk_gptdriver=${apkIdentifier(instrumentation.context.packageName)} run_id=$runId"
        )

        check(awaitTokens()) { "No randomized tokens within ${TOKEN_MS}ms; the install/open did not complete" }
        setAttributionFull()

        val link = generateLinkWithRetry(runId)
        check(link.startsWith("https://")) { "Expected an https link, got '$link'" }

        deliverWarmAndConfirm(link, runId)

        // Background, return and the verdict read are added in later work; nothing past
        // delivery is asserted here yet.
        Log.i(TAG, "delivery and chained open confirmed for run_id=$runId")
    }

    private fun setAttributionFull() {
        scenario!!.onActivity {
            Branch.getInstance()
                .setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL) { _, _, _ -> }
        }
        // FULL -> FULL never invokes the callback (Branch.java:2184-2195); the preference
        // write is synchronous, so that is checked here instead of waiting on it.
        check(
            PrefHelper.getInstance(context).getConsumerProtectionAttributionLevel() ==
                Defines.BranchAttributionLevel.FULL
        ) { "Attribution level not FULL after setConsumerProtectionAttributionLevel" }
    }

    private fun generateLinkWithRetry(runId: String): String {
        var lastError: Throwable? = null
        repeat(LINK_RETRIES) { attempt ->
            try {
                return generateLink(runId)
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Link generation attempt ${attempt + 1}/$LINK_RETRIES failed: ${e.message}")
            }
        }
        throw BranchApiUnreachableException(
            "Link generation failed after $LINK_RETRIES attempts: ${lastError?.message}",
            lastError
        )
    }

    private fun generateLink(runId: String): String {
        val latch = CountDownLatch(1)
        var url: String? = null
        var error: BranchError? = null
        val properties = LinkProperties()
            .addControlParameter(KEY_SCENARIO, SCENARIO_NAME)
            .addControlParameter(KEY_RUN_ID, runId)
        scenario!!.onActivity { activity ->
            BranchUniversalObject()
                .setCanonicalIdentifier("l1/$SCENARIO_NAME/$runId")
                .generateShortUrl(activity, properties, { generated, failure ->
                    url = generated
                    error = failure
                    latch.countDown()
                }, false)
        }
        check(latch.await(LINK_MS, TimeUnit.MILLISECONDS)) { "No link within ${LINK_MS}ms" }
        check(error == null) { "Link generation failed: ${error?.message}" }
        return url.orEmpty()
    }

    private fun deliverWarmAndConfirm(link: String, runId: String) {
        val markerLine = currentLineCount()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            setClassName(context, "io.branch.branchandroidtestbed.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
        check(pollForRunId(markerLine, runId, LINK_DELIVERY_MS)) {
            "No 'Deep link params:' line carrying $runId within ${LINK_DELIVERY_MS}ms"
        }
        check(awaitCaptureFrom(REQUEST_OPEN_SUCCEEDED, markerLine, CHAINED_OPEN_MS)) {
            "Chained open did not reach '$REQUEST_OPEN_SUCCEEDED' within ${CHAINED_OPEN_MS}ms"
        }
    }

    private fun hasTokens(): Boolean {
        val prefs = PrefHelper.getInstance(context)
        return prefs.randomizedDeviceToken != PrefHelper.NO_STRING_VALUE &&
            prefs.randomizedBundleToken != PrefHelper.NO_STRING_VALUE
    }

    private fun awaitTokens(): Boolean {
        val deadline = System.currentTimeMillis() + TOKEN_MS
        while (!hasTokens()) {
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(TOKEN_POLL_MS)
        }
        return true
    }

    private fun currentLineCount(): Int = if (captureFile.exists()) captureFile.readLines().size else 0

    private fun linesSince(fromLine: Int): List<String> =
        if (captureFile.exists()) captureFile.readLines().drop(fromLine) else emptyList()

    private fun awaitCaptureFrom(marker: String, fromLine: Int, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (linesSince(fromLine).any { it.contains(marker) }) return true
            Thread.sleep(POLL_MS)
        }
        return linesSince(fromLine).any { it.contains(marker) }
    }

    private fun pollForRunId(fromLine: Int, runId: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (linesSince(fromLine).any { it.contains("Deep link params:") && it.contains(runId) }) return true
            Thread.sleep(POLL_MS)
        }
        return linesSince(fromLine).any { it.contains("Deep link params:") && it.contains(runId) }
    }

    private fun apkIdentifier(pkg: String): String = try {
        val info = context.packageManager.getPackageInfo(pkg, 0)
        "$pkg:${info.versionName}:${info.lastUpdateTime}"
    } catch (e: Exception) {
        "$pkg:unavailable(${e.message})"
    }

    /** A genuinely unreachable Branch API, kept distinct so a later read can classify it apart from an ordinary failure. */
    private class BranchApiUnreachableException(message: String, cause: Throwable?) : RuntimeException(message, cause)

    private companion object {
        const val TAG = "NoStickinessWireTest"
        const val SCENARIO_NAME = "no_stickiness"
        const val KEY_SCENARIO = "l1_scenario"
        const val KEY_RUN_ID = "l1_run_id"
        const val REQUEST_OPEN_SUCCEEDED = "RequestOpen Succeeded"
        const val TOKEN_MS = 20_000L
        const val TOKEN_POLL_MS = 250L
        const val LINK_MS = 15_000L
        const val LINK_DELIVERY_MS = 20_000L
        const val CHAINED_OPEN_MS = 20_000L
        const val POLL_MS = 200L
        const val LINK_RETRIES = 3
    }
}
