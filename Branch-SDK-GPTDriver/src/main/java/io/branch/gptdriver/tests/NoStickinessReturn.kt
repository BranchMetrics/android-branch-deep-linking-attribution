package io.branch.gptdriver.tests

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.branch.branchandroidtestbed.MainActivity
import io.branch.referral.Branch
import java.io.File
import java.util.regex.Pattern
import org.junit.After
import org.junit.Test

/** no_stickiness: warm delivery, a real background, a recents return with no new intent, verdict from the accessor read after the return. Prints result/reason as an instrumentation status. */
class NoStickinessReturn {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val captureFile = File(context.filesDir, "branchlogs.txt")
    private val uiDevice = UiDevice.getInstance(instrumentation)
    private var scenario: ActivityScenario<MainActivity>? = null

    // Set once the recents tap decides which poll path it took; read back into the reported
    // reason on both pass and fail, so CI output shows the path without a log pull.
    private var recentsPath: String? = null
    private var cardWaitMs: Long? = null

    // Test-only switches for the forced-mismatch runs; never set by the real gate.
    private val forceZeroCards = InstrumentationRegistry.getArguments().getString(ARG_FORCE_ZERO) == "true"
    private val forceExtraCard = InstrumentationRegistry.getArguments().getString(ARG_FORCE_EXTRA) == "true"

    @After
    fun tearDown() {
        // Do not close(): once backgrounded and returned to, the scenario cannot reach
        // DESTROYED through the rule's teardown. The runner reclaims the process.
    }

    @Test
    fun noStickinessAfterReturn() {
        val runId = System.currentTimeMillis().toString()
        try {
            runScenario(runId)
        } catch (e: NoStickinessSupport.BranchApiUnreachableException) {
            if (NoStickinessSupport.isBranchApiReachable()) {
                // The exception fired but a fresh independent check says the API answers: not an
                // outage, so this is an ordinary failure, never not_run.
                NoStickinessSupport.reportResult("fail", reasonWithPath("link generation failed, api reachable: ${e.message}"))
                throw e
            }
            NoStickinessSupport.reportResult("not_run", "branch api unreachable: ${e.message}")
        } catch (e: Throwable) {
            NoStickinessSupport.reportResult("fail", reasonWithPath(e.message ?: e.javaClass.simpleName))
            throw e
        }
    }

    // Appends the decided recents path only when one was reached; the failure reason stays
    // first so the cap on reportResult never trims it away.
    private fun reasonWithPath(reason: String): String =
        recentsPath?.let { "$reason recents_path=$it" } ?: reason

    private fun runScenario(runId: String) {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Log.i(
            TAG,
            "apk_testbed=${NoStickinessSupport.apkIdentifier(context, context.packageName)} " +
                "apk_gptdriver=${NoStickinessSupport.apkIdentifier(context, instrumentation.context.packageName)} run_id=$runId"
        )

        check(NoStickinessSupport.awaitTokens(context)) {
            "No randomized tokens within the token wait; the install/open did not complete"
        }
        NoStickinessSupport.setAttributionFull(scenario!!, context)

        val link = NoStickinessSupport.generateLinkWithRetry(scenario!!, runId)
        check(link.startsWith("https://")) { "Expected an https link, got '$link'" }

        NoStickinessSupport.deliverWarmAndConfirm(context, captureFile, link, runId)

        var activity: Activity? = null
        var intentSnapshot: Uri? = null
        scenario!!.onActivity {
            activity = it
            intentSnapshot = it.intent?.data
        }
        checkNotNull(activity) { "activity reference unavailable after delivery" }

        // Marked before the forced-extra-card launch: that launch backgrounds this activity for
        // real, so the stop dispatch it causes must still count as the one this poll waits for.
        val preBackgroundMarker = NoStickinessSupport.currentLineCount(captureFile)
        if (forceExtraCard) {
            launchSecondAppAndAwaitVisible()
        }

        uiDevice.pressHome()
        check(
            NoStickinessSupport.awaitCaptureFrom(
                captureFile, NoStickinessSupport.ONSTOP_DISPATCH_LINE, preBackgroundMarker, STOP_DISPATCH_MS
            )
        ) {
            "no stop-dispatch line within ${STOP_DISPATCH_MS}ms after pressHome"
        }
        check(!Branch.getInstance().requestQueue_.containsDeepLinkOrOpen()) {
            "queue held a deep link resolution or open at the stop dispatch"
        }
        // Diagnostic only; the pass/fail verdict never reads this one.
        Log.i(TAG, "diagnostic accessor after background: ${Branch.getInstance().getLatestReferringParams()}")

        val returnMarker = NoStickinessSupport.currentLineCount(captureFile)
        check(!mainActivityResumed()) {
            "MainActivity already RESUMED before the recents tap; the return would settle on nothing"
        }
        tapExactlyOneRecentCard()

        awaitReturnSettled()

        val sinceReturn = NoStickinessSupport.linesSince(captureFile, returnMarker)
        val deeplinkPosts = sinceReturn.count { it.contains("posting to") && it.contains("/v3/deeplink") }
        check(deeplinkPosts == 0) { "expected 0 /v3/deeplink posts since the return, saw $deeplinkPosts" }
        check(sinceReturn.none { it.contains("Deep link params:") || it.contains("Deep link error:") }) {
            "expected no new-intent handling since the return, saw one"
        }

        var dataAfterReturn: Uri? = null
        scenario!!.onActivity { dataAfterReturn = it.intent?.data }
        check(dataAfterReturn == intentSnapshot) {
            "activity intent data changed on return: was $intentSnapshot, now $dataAfterReturn"
        }

        // Diagnostic only, per spec: the return's own open body never decides the verdict.
        val openBody = NoStickinessSupport.openBodyDiagnosticSince(captureFile, returnMarker)
        Log.i(TAG, "diagnostic return open body: $openBody")
        // A recents return is not a session start on the beta, so it posts no open.
        val openPosts = sinceReturn.count { it.contains("posting to") && it.contains("/v3/events/open") }
        check(openPosts == 0) { "expected 0 /v3/events/open posts since the return, saw $openPosts" }

        // The verdict: the accessor read after the return, and nothing else.
        val referring = Branch.getInstance().getLatestReferringParams()
        val survivors = NoStickinessSupport.STICKY_FIELDS.filter { referring.has(it) }
        check(survivors.isEmpty()) { "accessor after return carried ${survivors.joinToString(",")}" }
        NoStickinessSupport.reportResult("pass", "recents_path=$recentsPath card_ms=$cardWaitMs")
    }

    // The return logs no SDK line, so it settles on MainActivity RESUMED (from the lifecycle
    // monitor; the scenario stops tracking after a new intent), then on a quiet window.
    private fun awaitReturnSettled() {
        val start = System.currentTimeMillis()
        val deadline = start + RESUMED_MS
        while (!mainActivityResumed() && System.currentTimeMillis() < deadline) {
            Thread.sleep(NoStickinessSupport.POLL_MS)
        }
        check(mainActivityResumed()) {
            "MainActivity not RESUMED within ${RESUMED_MS}ms after the recents tap"
        }
        Log.i(TAG, "return resumed resumedMs=${System.currentTimeMillis() - start}")
        NoStickinessSupport.awaitQuiescentLineCount(captureFile, QUIESCENCE_MS)
    }

    // Pass/fail on card count comes from dumpsys `Activities=[]`, never from the launcher's
    // card carousel, which this emulator skin renders unreliably.
    private fun tapExactlyOneRecentCard() {
        try {
            tapExactlyOneRecentCardOrThrow()
        } catch (e: Throwable) {
            NoStickinessSupport.captureFailureArtifacts(uiDevice, TAG)
            throw e
        }
    }

    private fun tapExactlyOneRecentCardOrThrow() {
        val recentsPressedAt = System.currentTimeMillis()
        uiDevice.pressRecentApps()
        uiDevice.wait(Until.hasObject(CARD_SELECTOR), RECENTS_WAIT_MS)
        if (forceZeroCards) {
            dismissAllRecentCards()
        }
        val liveTasks = NoStickinessSupport.liveNonHomeTaskCount(instrumentation)
        check(liveTasks == 1) {
            "expected exactly one live task in dumpsys activity recents, found $liveTasks; ${diagnostic()}"
        }
        var candidates = awaitRenderedCard()
        // liveTasks==1 already ruled out the extra-card case: an empty read here means either
        // our own app (re-press) or the launcher (keep waiting).
        var path = "immediate"
        if (candidates.isEmpty()) {
            if (stillInOwnApp()) {
                path = "re_press"
                uiDevice.pressRecentApps()
                candidates = awaitRenderedCard()
            } else {
                path = "launcher_extended_wait"
                candidates = awaitRenderedCard(LAUNCHER_EXTENDED_WAIT_MS)
            }
        }
        val cardWaitElapsedMs = System.currentTimeMillis() - recentsPressedAt
        recentsPath = path
        cardWaitMs = cardWaitElapsedMs
        Log.i(TAG, "recents path=$path cardWaitMs=$cardWaitElapsedMs found=${candidates.isNotEmpty()}")
        check(candidates.isNotEmpty()) {
            "one live task in recents but no rendered card matched the snapshot selector; ${diagnostic()}"
        }
        candidates[0].click()
    }

    private fun mainActivityResumed(): Boolean {
        var resumed = false
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).any { it is MainActivity }
        }
        return resumed
    }

    private fun stillInOwnApp(): Boolean = uiDevice.currentPackageName == context.packageName

    private fun diagnostic(): String =
        NoStickinessSupport.cardCountDiagnostic(uiDevice, instrumentation, SNAPSHOT_SELECTOR)

    // An empty read here is not trusted as a real zero; keeps sampling until the deadline.
    private fun awaitRenderedCard(timeoutMs: Long = RECENTS_WAIT_MS): List<UiObject2> {
        val deadline = System.currentTimeMillis() + timeoutMs
        var candidates = stableCards()
        while (candidates.isEmpty() && System.currentTimeMillis() < deadline) {
            candidates = stableCards()
        }
        return candidates
    }

    // Requires two consecutive equal-sized reads before trusting the count.
    private fun stableCards(): List<UiObject2> {
        var last = uiDevice.findObjects(CARD_SELECTOR)
        val deadline = System.currentTimeMillis() + STABLE_READ_MS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(STABLE_POLL_MS)
            val next = uiDevice.findObjects(CARD_SELECTOR)
            if (next.size == last.size) return next
            last = next
        }
        return last
    }

    // Test-only: swipes every visible card away; retries until none remain or the deadline passes.
    private fun dismissAllRecentCards() {
        val deadline = System.currentTimeMillis() + RECENTS_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            val remaining = stableCards()
            if (remaining.isEmpty()) return
            remaining.forEach { it.swipe(Direction.UP, 1.0f) }
            uiDevice.wait(Until.gone(CARD_SELECTOR), DISMISS_SETTLE_MS)
        }
    }

    // Test-only: leaves a second app's task in recents, to exercise the many-card fail path.
    private fun launchSecondAppAndAwaitVisible() {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val settingsPackage = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
            ?: DEFAULT_SETTINGS_PACKAGE
        context.startActivity(intent)
        check(uiDevice.wait(Until.hasObject(By.pkg(settingsPackage)), SETTINGS_VISIBLE_MS)) {
            "Settings did not become visible within ${SETTINGS_VISIBLE_MS}ms"
        }
    }

    private companion object {
        const val TAG = "NoStickinessReturn"
        const val ARG_FORCE_ZERO = "no_stickiness_force_zero_cards"
        const val ARG_FORCE_EXTRA = "no_stickiness_force_extra_card"
        const val DEFAULT_SETTINGS_PACKAGE = "com.android.settings"
        const val STOP_DISPATCH_MS = 5_000L
        const val RECENTS_WAIT_MS = 5_000L
        const val LAUNCHER_EXTENDED_WAIT_MS = 10_000L
        const val STABLE_READ_MS = 1_500L
        const val STABLE_POLL_MS = 250L
        const val DISMISS_SETTLE_MS = 1_500L
        const val RESUMED_MS = 15_000L
        const val QUIESCENCE_MS = 5_000L
        const val SETTINGS_VISIBLE_MS = 5_000L
        val SNAPSHOT_SELECTOR: BySelector = By.res(Pattern.compile(".*:id/snapshot$"))
        val ICON_SELECTOR: BySelector = By.res(Pattern.compile(".*:id/icon$"))
        // Matches only a fully-rendered card (snapshot + icon), never a bare icon-less sliver.
        val CARD_SELECTOR: BySelector = By.hasChild(SNAPSHOT_SELECTOR).hasChild(ICON_SELECTOR)
    }
}
