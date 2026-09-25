package io.branch.gptdriver.tests

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.branch.branchandroidtestbed.MainActivity
import io.branch.referral.Branch
import java.io.File
import java.util.regex.Pattern
import org.junit.After
import org.junit.Test

/** no_stickiness: backgrounds for real, returns through recents with no new intent, checks the return is clean. The accessor verdict is added separately. */
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
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val runId = System.currentTimeMillis().toString()
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
        val mainActivity = checkNotNull(activity) { "activity reference unavailable after delivery" }

        stopOtherApps(mainActivity.taskId)
        if (forceExtraCard) {
            launchSecondAppAndAwaitVisible()
        }

        uiDevice.pressHome()
        // This step's own bounded background wait; the process stop-dispatch wait is added later.
        check(waitForStopped(mainActivity, HOME_BACKGROUND_MS)) {
            "activity did not reach STOPPED within ${HOME_BACKGROUND_MS}ms after pressHome"
        }
        val returnMarker = NoStickinessSupport.currentLineCount(captureFile)
        tapExactlyOneRecentCard()

        val signal = awaitForegroundSignal(returnMarker)
        check(signal != null) {
            "no foreground signal (open succeeded or onStart dispatch) since the return within ${RETURN_SIGNAL_MS}ms"
        }
        Log.i(TAG, "foreground signal since return: $signal")

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

        // Diagnostic only; the pass/fail verdict on this read is added separately.
        Log.i(TAG, "diagnostic accessor after return: ${Branch.getInstance().getLatestReferringParams()}")
    }

    private fun awaitForegroundSignal(fromLine: Int): String? {
        val deadline = System.currentTimeMillis() + RETURN_SIGNAL_MS
        while (System.currentTimeMillis() < deadline) {
            foregroundSignalIn(fromLine)?.let { return it }
            Thread.sleep(NoStickinessSupport.POLL_MS)
        }
        return foregroundSignalIn(fromLine)
    }

    private fun foregroundSignalIn(fromLine: Int): String? {
        val lines = NoStickinessSupport.linesSince(captureFile, fromLine)
        return when {
            lines.any { it.contains(NoStickinessSupport.REQUEST_OPEN_SUCCEEDED) } -> "open_succeeded"
            lines.any { it.contains(NoStickinessSupport.ONSTART_DISPATCH_LINE) } -> "onstart_dispatch"
            else -> null
        }
    }

    private fun tapExactlyOneRecentCard() {
        uiDevice.pressRecentApps()
        uiDevice.wait(Until.hasObject(SNAPSHOT_SELECTOR), RECENTS_WAIT_MS)
        if (forceZeroCards) {
            dismissAllRecentCards()
        }
        val candidates = uiDevice.findObjects(SNAPSHOT_SELECTOR)
        check(candidates.size == 1) {
            "expected exactly one recents card matching the snapshot selector, found ${candidates.size}"
        }
        candidates[0].click()
    }

    // Test-only: swipes every visible card away, to exercise the zero-card fail path. A card
    // swipe is the ordinary dismiss gesture, unlike pm clear, so the process survives it.
    private fun dismissAllRecentCards() {
        uiDevice.findObjects(SNAPSHOT_SELECTOR).forEach { it.swipe(Direction.UP, 1.0f) }
        uiDevice.wait(Until.gone(SNAPSHOT_SELECTOR), RECENTS_WAIT_MS)
    }

    private fun waitForStopped(activity: Activity, timeoutMs: Long): Boolean {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (stageOnMainThread(monitor, activity) == Stage.STOPPED) return true
            Thread.sleep(NoStickinessSupport.POLL_MS)
        }
        return stageOnMainThread(monitor, activity) == Stage.STOPPED
    }

    // The lifecycle monitor only allows reading stage from the main thread.
    private fun stageOnMainThread(monitor: ActivityLifecycleMonitor, activity: Activity): Stage {
        var stage = Stage.PRE_ON_CREATE
        instrumentation.runOnMainSync { stage = monitor.getLifecycleStageOf(activity) }
        return stage
    }

    // dumpsys activity recents (not activities): the launcher renders cards from its whole
    // recents history, including tasks whose process already died, not just live ones. `am
    // task` has no remove subcommand on this image; `am stack remove` takes a task ID directly,
    // one call per single-task stack, no package name needed.
    private fun stopOtherApps(keepTaskId: Int) {
        val dump = shellOutput("dumpsys activity recents")
        RECENT_HEADER.findAll(dump)
            .mapNotNull { m ->
                val (taskId, type) = m.destructured
                if (type == "home" || taskId.toInt() == keepTaskId) null else taskId
            }
            .distinct()
            .forEach { taskId -> shellOutput("am stack remove $taskId") }
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
        const val HOME_BACKGROUND_MS = 5_000L
        const val RECENTS_WAIT_MS = 5_000L
        const val RETURN_SIGNAL_MS = 15_000L
        const val SETTINGS_VISIBLE_MS = 5_000L
        val SNAPSHOT_SELECTOR: BySelector = By.res(Pattern.compile(".*:id/snapshot$"))
        val RECENT_HEADER = Regex("""Recent #\d+: Task\{\S+ #(\d+) type=(\S+)""")
    }
}
