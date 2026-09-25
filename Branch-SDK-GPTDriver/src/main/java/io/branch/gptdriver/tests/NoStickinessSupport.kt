package io.branch.gptdriver.tests

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
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

/** Shared no_stickiness helpers: token wait, attribution, link generation, warm delivery, capture-file polling. */
internal object NoStickinessSupport {

    const val SCENARIO_NAME = "no_stickiness"
    const val KEY_SCENARIO = "l1_scenario"
    const val KEY_RUN_ID = "l1_run_id"
    const val REQUEST_OPEN_SUCCEEDED = "RequestOpen Succeeded"
    const val ONSTART_DISPATCH_LINE = "BranchProcessLifecycleObserver onStart: process foregrounded"
    const val POLL_MS = 200L

    private const val TAG = "NoStickinessSupport"
    private const val TOKEN_MS = 20_000L
    private const val TOKEN_POLL_MS = 250L
    private const val LINK_MS = 15_000L
    private const val LINK_DELIVERY_MS = 20_000L
    private const val CHAINED_OPEN_MS = 20_000L
    private const val LINK_RETRIES = 3

    fun hasTokens(context: Context): Boolean {
        val prefs = PrefHelper.getInstance(context)
        return prefs.randomizedDeviceToken != PrefHelper.NO_STRING_VALUE &&
            prefs.randomizedBundleToken != PrefHelper.NO_STRING_VALUE
    }

    fun awaitTokens(context: Context): Boolean {
        val deadline = System.currentTimeMillis() + TOKEN_MS
        while (!hasTokens(context)) {
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(TOKEN_POLL_MS)
        }
        return true
    }

    fun setAttributionFull(scenario: ActivityScenario<MainActivity>, context: Context) {
        scenario.onActivity {
            Branch.getInstance()
                .setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL) { _, _, _ -> }
        }
        // FULL -> FULL never invokes the callback (Branch.java:2184-2195); the preference write is synchronous.
        check(
            PrefHelper.getInstance(context).getConsumerProtectionAttributionLevel() ==
                Defines.BranchAttributionLevel.FULL
        ) { "Attribution level not FULL after setConsumerProtectionAttributionLevel" }
    }

    fun generateLinkWithRetry(scenario: ActivityScenario<MainActivity>, runId: String): String {
        var lastError: Throwable? = null
        repeat(LINK_RETRIES) { attempt ->
            try {
                return generateLink(scenario, runId)
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

    private fun generateLink(scenario: ActivityScenario<MainActivity>, runId: String): String {
        val latch = CountDownLatch(1)
        var url: String? = null
        var error: BranchError? = null
        val properties = LinkProperties()
            .addControlParameter(KEY_SCENARIO, SCENARIO_NAME)
            .addControlParameter(KEY_RUN_ID, runId)
        scenario.onActivity { activity ->
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

    fun deliverWarmAndConfirm(context: Context, captureFile: File, link: String, runId: String) {
        val markerLine = currentLineCount(captureFile)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            setClassName(context, "io.branch.branchandroidtestbed.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
        check(pollForRunId(captureFile, markerLine, runId, LINK_DELIVERY_MS)) {
            "No 'Deep link params:' line carrying $runId within ${LINK_DELIVERY_MS}ms"
        }
        check(awaitCaptureFrom(captureFile, REQUEST_OPEN_SUCCEEDED, markerLine, CHAINED_OPEN_MS)) {
            "Chained open did not reach '$REQUEST_OPEN_SUCCEEDED' within ${CHAINED_OPEN_MS}ms"
        }
    }

    fun currentLineCount(captureFile: File): Int = if (captureFile.exists()) captureFile.readLines().size else 0

    fun linesSince(captureFile: File, fromLine: Int): List<String> =
        if (captureFile.exists()) captureFile.readLines().drop(fromLine) else emptyList()

    fun awaitCaptureFrom(captureFile: File, marker: String, fromLine: Int, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (linesSince(captureFile, fromLine).any { it.contains(marker) }) return true
            Thread.sleep(POLL_MS)
        }
        return linesSince(captureFile, fromLine).any { it.contains(marker) }
    }

    fun pollForRunId(captureFile: File, fromLine: Int, runId: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (linesSince(captureFile, fromLine).any { it.contains("Deep link params:") && it.contains(runId) }) return true
            Thread.sleep(POLL_MS)
        }
        return linesSince(captureFile, fromLine).any { it.contains("Deep link params:") && it.contains(runId) }
    }

    fun apkIdentifier(context: Context, pkg: String): String = try {
        val info = context.packageManager.getPackageInfo(pkg, 0)
        "$pkg:${info.versionName}:${info.lastUpdateTime}"
    } catch (e: Exception) {
        "$pkg:unavailable(${e.message})"
    }

    /** A genuinely unreachable Branch API, kept distinct so a later read can classify it apart from an ordinary failure. */
    class BranchApiUnreachableException(message: String, cause: Throwable?) : RuntimeException(message, cause)
}
