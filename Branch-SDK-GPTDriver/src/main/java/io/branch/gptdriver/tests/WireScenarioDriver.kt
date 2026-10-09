package io.branch.gptdriver.tests

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * The steps the wire drivers share: launch the TestBed, optionally generate a link (W1 and W2
 * do, H2 does not), then deliver a URI through startActivity, either after sending the app to
 * the background ([background] then [deliver], W1WarmHttpsWireTest and
 * W2WarmUriSchemeWireTest) or while the activity is resumed ([deliverHot],
 * H2HotUriSchemeWireTest).
 *
 * Each step waits for something it can observe: the TestBed's own capture file
 * (branchlogs.txt, read in this process, which is the app's), the short-URL field, or the
 * activity's lifecycle callbacks. After each step the number of requests posted must also stay
 * unchanged for [QUIET_MS]. Request lines are counted, not capture bytes, so a VERBOSE log line
 * that is not a request does not restart the window. That window is the one place a sleep
 * stands in for a condition: it waits for requests that must not be there (a second open, a
 * stray event), and no signal says they will not come. It is what lets the contract's exact
 * counts see an extra request.
 *
 * The window after delivery is [FINAL_QUIET_MS], 12 s, as long as the fixed capture window the
 * earlier driver used. The bound is therefore: a duplicate open or stray event up to 12 s after
 * the last request is inside the capture, one later than that is not seen. The windows between
 * the other steps stay at [QUIET_MS].
 *
 * Counts are taken against a baseline: read at [launch], so a capture that already holds lines
 * (no CLEAR_LOG) does not satisfy a wait early, and read again when a URI is sent, so the
 * delivery's counts start from what the capture holds then.
 *
 * Residual risk: a launch request that reaches the wire more than [QUIET_MS] after the
 * launch's last post would land in the delivery's capture and push its counts over the
 * contract's. That is a false red, never a false green.
 *
 * The scenario is never closed here, and the drivers use no ActivityScenarioRule, for the same
 * reason: once a new intent has arrived through startActivity the scenario has lost lifecycle
 * control, so close() throws "Activity never becomes DESTROYED". DeepLinkWarmOpenHybridTest hit
 * the same wall. The runner cleans the activity up.
 */
internal class WireScenarioDriver {

    private var scenario: ActivityScenario<MainActivity>? = null
    private var baseline = emptyMap<String, Int>()
    private val lifecycle = MainActivityLifecycle()

    /** Starts the TestBed and waits for its launch to reach the wire: a deeplink and an open. */
    fun launch() {
        baseline = ENDPOINTS.associateWith { posts(it) }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        (app as Application).registerActivityLifecycleCallbacks(lifecycle)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        awaitPosts(DEEPLINK, OPEN)
        awaitQuiet()
        val launchOpen = launchOpenBody(captureFile())
        check(launchOpen != null && launchOpen.contains(TOKEN_FIELD)) {
            "no randomized_bundle_token on the launch open; run cold_https first"
        }
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
        awaitQuiet()
        return field
    }

    /**
     * Presses Home and asserts the activity went to the background: it must report onStop and
     * must not be destroyed. Without this a Home press that did not stop the activity, or a
     * device that destroys it ("Don't keep activities"), would still produce a capture, and
     * the scenario would no longer be a link arriving through onNewIntent on a stopped,
     * living activity.
     */
    fun background() {
        val stopsBefore = lifecycle.stops.get()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
        await("MainActivity did not stop after Home") {
            lifecycle.stops.get() > stopsBefore || lifecycle.destroys.get() > 0
        }
        assertBackgrounded()
        // The window must exceed ProcessLifecycleOwner's 700 ms stop delay: that delay is what
        // fires BranchProcessLifecycleObserver.onStop, which clears sessionParams and the saved
        // launch link, and it has to have fired before the link is delivered.
        awaitQuiet()
    }

    /**
     * Sets the capture so far aside, for a scenario whose contract judges only its delivery
     * (hot_uriScheme counts one deeplink and one open, not the launch's). [launch] owns the
     * arrival precondition (a deeplink and an open, then quiet), so it is not repeated here.
     * What this asserts can fail on its own: the rename's result, and that the set-aside file
     * holds the launch's /v3/events/open carrying randomized_bundle_token, so the pair the
     * contract no longer sees is the pair that was meant to leave it. [deliver] and
     * [deliverHot] take their baseline after this, so their counts start from the empty file.
     * The file stays at branchlogs.preclear.txt for diagnosis through run-as; the L1 script
     * does not pull it.
     */
    fun setCaptureAside() {
        val aside = File(captureFile().parentFile, ASIDE_FILE)
        aside.delete()
        check(captureFile().renameTo(aside)) { "could not move $CAPTURE_FILE to $ASIDE_FILE" }
        val launchOpen = launchOpenBody(aside)
        check(launchOpen != null && launchOpen.contains(TOKEN_FIELD)) {
            "the set-aside $ASIDE_FILE holds no launch /v3/events/open with randomized_bundle_token"
        }
    }

    /** The first open body past the baseline [launch] read, from [file], or null. */
    private fun launchOpenBody(file: File) =
        openBodies(file).drop(baseline.getValue(OPEN)).firstOrNull()

    private fun assertBackgrounded() {
        check(lifecycle.destroys.get() == 0) {
            "MainActivity was destroyed, not stopped. The link would reach onCreate, not onNewIntent"
        }
        check(lifecycle.stopped) { "MainActivity is not stopped, so this would not be a warm link" }
    }

    /**
     * Delivers [uri] and waits for the deeplink and open it causes, then for quiet. A request
     * that never comes is not an error here: the contract that judges the capture names what
     * is missing, and stopping this driver early would skip that judgement.
     */
    fun deliver(uri: String) {
        assertBackgrounded()
        send(uri)
        check(lifecycle.destroys.get() == 0) {
            "MainActivity was destroyed during delivery, so the link did not reach a living " +
                "activity through onNewIntent"
        }
    }

    /**
     * Delivers [uri] while MainActivity is resumed, with no background step, and waits as
     * [deliver] does. Asserts MainActivity is RESUMED before sending and again after the final
     * quiet, and that it never stopped or died since [launch], so the link reached onNewIntent
     * hot and not warm.
     */
    fun deliverHot(uri: String) {
        check(lifecycle.resumed) {
            "MainActivity is not resumed before delivery, so this would not be a hot link"
        }
        send(uri)
        check(lifecycle.resumed) {
            "MainActivity is not resumed after delivery, so the link did not land hot"
        }
        check(lifecycle.stops.get() == 0 && lifecycle.destroys.get() == 0) {
            "MainActivity left the foreground during the scenario, so the delivery was not hot"
        }
    }

    private fun send(uri: String) {
        baseline = ENDPOINTS.associateWith { posts(it) }
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
        poll(WAIT_MS) { arrived(DEEPLINK) && arrived(OPEN) }
        awaitQuiet(FINAL_QUIET_MS)
    }

    private fun captureFile() =
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, CAPTURE_FILE)

    /** How many requests to [endpoint] the capture holds, or to any endpoint when null. */
    private fun posts(endpoint: String? = null): Int {
        val file = captureFile()
        if (!file.exists()) return 0
        return file.readLines().count {
            it.contains("posting to ") && (endpoint == null || it.trim().endsWith(endpoint))
        }
    }

    /** The body line of each open the capture holds, in order. */
    private fun openBodies(file: File): List<String> {
        if (!file.exists()) return emptyList()
        val bodies = mutableListOf<String>()
        var pending = false
        for (line in file.readLines()) {
            if (line.contains("posting to ")) pending = line.trim().endsWith(OPEN)
            else if (pending && line.startsWith("Post value = ")) {
                bodies.add(line)
                pending = false
            }
        }
        return bodies
    }

    private fun arrived(endpoint: String) = posts(endpoint) > baseline.getValue(endpoint)

    private fun awaitPosts(vararg endpoints: String) =
        await("no ${endpoints.joinToString(" and ")} in the capture") { endpoints.all { arrived(it) } }

    /** Returns once no request has been posted for [quietMs]. */
    private fun awaitQuiet(quietMs: Long = QUIET_MS) {
        var count = posts()
        var since = System.currentTimeMillis()
        await("requests were still being posted after ${WAIT_MS}ms") {
            val now = posts()
            if (now != count) {
                count = now
                since = System.currentTimeMillis()
            }
            System.currentTimeMillis() - since >= quietMs
        }
    }

    private fun await(failure: String, condition: () -> Boolean) =
        check(poll(WAIT_MS, condition)) { "$failure (waited ${WAIT_MS}ms)" }

    private fun poll(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(POLL_MS)
        }
        return true
    }

    /**
     * Follows MainActivity only: whether it is stopped or resumed now, and how often it stopped
     * or died.
     */
    private class MainActivityLifecycle : Application.ActivityLifecycleCallbacks {
        val stops = AtomicInteger()
        val destroys = AtomicInteger()
        @Volatile var stopped = false
        @Volatile var resumed = false

        override fun onActivityStarted(activity: Activity) {
            if (activity is MainActivity) stopped = false
        }

        override fun onActivityStopped(activity: Activity) {
            if (activity is MainActivity) {
                stops.incrementAndGet()
                stopped = true
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity is MainActivity) destroys.incrementAndGet()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {
            if (activity is MainActivity) resumed = true
        }

        override fun onActivityPaused(activity: Activity) {
            if (activity is MainActivity) resumed = false
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }

    private companion object {
        const val DEEPLINK = "/v3/deeplink"
        const val OPEN = "/v3/events/open"
        val ENDPOINTS = listOf(DEEPLINK, OPEN)
        const val TOKEN_FIELD = "\"randomized_bundle_token\""
        const val CAPTURE_FILE = "branchlogs.txt"
        const val ASIDE_FILE = "branchlogs.preclear.txt"
        const val LINK_TIMEOUT_MS = 30_000L
        const val WAIT_MS = 30_000L
        const val POLL_MS = 250L
        const val QUIET_MS = 3_000L
        const val FINAL_QUIET_MS = 12_000L
    }
}
