package io.branch.gptdriver.tests

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
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
                NoStickinessSupport.reportResult("fail", "link generation failed, api reachable: ${e.message}")
                throw e
            }
            NoStickinessSupport.reportResult("not_run", "branch api unreachable: ${e.message}")
        } catch (e: Throwable) {
            NoStickinessSupport.reportResult("fail", e.message ?: e.javaClass.simpleName)
            throw e
        }
    }

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
        check(!Branch.getInstance().requestQueue_.containsInstallOpenOrResolution()) {
            "queue held an install, open or resolution at the stop dispatch"
        }
        // Diagnostic only; the pass/fail verdict never reads this one.
        Log.i(TAG, "diagnostic accessor after background: ${Branch.getInstance().getLatestReferringParams()}")

        val returnMarker = NoStickinessSupport.currentLineCount(captureFile)
        tapExactlyOneRecentCard()

        val path = awaitReturnSettled(returnMarker)
        Log.i(TAG, "foreground signal path since return: $path")

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

        // The verdict: the accessor read after the return, and nothing else.
        val referring = Branch.getInstance().getLatestReferringParams()
        val survivors = NoStickinessSupport.STICKY_FIELDS.filter { referring.has(it) }
        check(survivors.isEmpty()) { "accessor after return carried ${survivors.joinToString(",")}" }
        NoStickinessSupport.reportResult("pass", "none")
    }

    // Primary: the return's own open completing is the real signal. The onStart dispatch line
    // logs before that open is even sent, so it is only trusted as a fallback, and only after a
    // quiet window, so a delayed /v3/deeplink post still has time to land before the checks below.
    private fun awaitReturnSettled(fromLine: Int): String {
        if (NoStickinessSupport.awaitCaptureFrom(captureFile, NoStickinessSupport.REQUEST_OPEN_SUCCEEDED, fromLine, RETURN_SIGNAL_MS)) {
            NoStickinessSupport.awaitQuiescentLineCount(captureFile, QUIESCENCE_MS)
            return "primary"
        }
        check(NoStickinessSupport.awaitCaptureFrom(captureFile, NoStickinessSupport.ONSTART_DISPATCH_LINE, fromLine, FALLBACK_SIGNAL_MS)) {
            "no foreground signal (open succeeded or onStart dispatch) since the return within ${RETURN_SIGNAL_MS + FALLBACK_SIGNAL_MS}ms"
        }
        Log.i(TAG, "primary open signal never arrived; using the onStart dispatch fallback, then waiting for quiet")
        NoStickinessSupport.awaitQuiescentLineCount(captureFile, QUIESCENCE_MS)
        return "fallback"
    }

    // The count that decides pass/fail comes from dumpsys, not from the launcher's card
    // carousel: this narrow emulator skin only ever renders ONE card at full size with its
    // icon at a time, live or not, so a second live task (the forceExtraCard path) peeks in
    // as an icon-less sliver indistinguishable, in the accessibility tree alone, from a stale
    // one. `Activities=[]` on a task is authoritative on both API 30 and 34, ghost-proof, and
    // unaffected by which card the carousel currently has centered.
    private fun tapExactlyOneRecentCard() {
        uiDevice.pressRecentApps()
        uiDevice.wait(Until.hasObject(CARD_SELECTOR), RECENTS_WAIT_MS)
        if (forceZeroCards) {
            dismissAllRecentCards()
        }
        val liveTasks = liveNonHomeTaskCount()
        check(liveTasks == 1) {
            "expected exactly one recents card matching the snapshot selector, found $liveTasks; ${cardCountDiagnostic()}"
        }
        val candidates = awaitRenderedCard()
        check(candidates.isNotEmpty()) {
            "one live task in recents but no rendered card matched the snapshot selector; ${cardCountDiagnostic()}"
        }
        candidates[0].click()
    }

    // dumpsys already proved exactly one live task exists; an empty stableCards() read here
    // is the launcher still animating the icon in, not a real zero, so this keeps sampling
    // past one stable-but-empty read instead of trusting it the way dismissAllRecentCards does.
    private fun awaitRenderedCard(): List<UiObject2> {
        val deadline = System.currentTimeMillis() + RECENTS_WAIT_MS
        var candidates = stableCards()
        while (candidates.isEmpty() && System.currentTimeMillis() < deadline) {
            candidates = stableCards()
        }
        return candidates
    }

    // A task with an empty Activities list is a stale recents entry the launcher still
    // remembers (a cleared or killed process outlives its card, on every API level tried);
    // it never counts as a live "other" task.
    private fun liveNonHomeTaskCount(): Int =
        TASK_BLOCK.findAll(shellOutput("dumpsys activity recents")).count { m ->
            val (_, type, activities) = m.destructured
            type != "home" && activities.isNotBlank()
        }

    // This launcher's recents list rebinds its views on a timer even while idle, so a single
    // point-in-time read can land in that gap and undercount. Two consecutive equal-sized reads
    // are trusted; a read that keeps changing falls through to the last one, still bounded.
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

    // Test-only: swipes every visible card away, to exercise the zero-card fail path. A card
    // swipe is the ordinary dismiss gesture, unlike pm clear, so the process survives it. Retried
    // for the same rebind-gap reason as stableCards(): a swipe issued into an empty read is a
    // silent no-op, so the loop keeps trying until nothing is left or the deadline passes.
    private fun dismissAllRecentCards() {
        val deadline = System.currentTimeMillis() + RECENTS_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            val remaining = stableCards()
            if (remaining.isEmpty()) return
            remaining.forEach { it.swipe(Direction.UP, 1.0f) }
            uiDevice.wait(Until.gone(CARD_SELECTOR), DISMISS_SETTLE_MS)
        }
    }

    // Printed only on a card-count mismatch, folded into the check() message so it reaches
    // the script's own result/reason output with no extra plumbing. Resource ids and content
    // descriptions only: no link tokens or keys ever appear in the recents tree or the task list.
    private fun cardCountDiagnostic(): String {
        val nodes = uiDevice.findObjects(SNAPSHOT_SELECTOR).joinToString(";") { n ->
            "res=${n.resourceName ?: "none"} desc=${n.contentDescription ?: ""}"
        }
        val tasks = TASK_BLOCK.findAll(shellOutput("dumpsys activity recents")).joinToString(",") { m ->
            val (taskId, type, activities) = m.destructured
            "$taskId:$type:${if (activities.isNotBlank()) "live" else "dead"}"
        }
        return "snapshots=[$nodes] tasks=[$tasks]"
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

    private fun shellOutput(command: String): String {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private companion object {
        const val TAG = "NoStickinessReturn"
        const val ARG_FORCE_ZERO = "no_stickiness_force_zero_cards"
        const val ARG_FORCE_EXTRA = "no_stickiness_force_extra_card"
        const val DEFAULT_SETTINGS_PACKAGE = "com.android.settings"
        const val STOP_DISPATCH_MS = 5_000L
        const val RECENTS_WAIT_MS = 5_000L
        const val STABLE_READ_MS = 1_500L
        const val STABLE_POLL_MS = 250L
        const val DISMISS_SETTLE_MS = 1_500L
        const val RETURN_SIGNAL_MS = 15_000L
        const val FALLBACK_SIGNAL_MS = 5_000L
        const val QUIESCENCE_MS = 3_000L
        const val SETTINGS_VISIBLE_MS = 5_000L
        val SNAPSHOT_SELECTOR: BySelector = By.res(Pattern.compile(".*:id/snapshot$"))
        val ICON_SELECTOR: BySelector = By.res(Pattern.compile(".*:id/icon$"))
        // Used only to find the card to tap once dumpsys has already decided pass/fail: a card
        // with both a rendered snapshot and its icon, as opposed to a bare icon-less sliver
        // (which this launcher also shows for the *current* card's own off-screen neighbours,
        // live or not, so this selector alone cannot decide "exactly one").
        val CARD_SELECTOR: BySelector = By.hasChild(SNAPSHOT_SELECTOR).hasChild(ICON_SELECTOR)
        // Spans to the task's own `Activities=[...]` line (DOTALL), not just its header: a task
        // whose process was cleared or killed keeps a recents entry with an empty activity list,
        // on every API level tried, unlike `sz=`/`StackId=`, which API 34's dumpsys omits
        // entirely. `.*?` before `type=` skips the `visible=<bool>` field API 30 inserts there
        // and API 34 does not.
        val TASK_BLOCK = Regex(
            """Recent #\d+: Task\{\S+ #(\d+) .*?type=(\S+).*?Activities=\[(.*?)\]""",
            RegexOption.DOT_MATCHES_ALL
        )
    }
}
