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
 * The steps W1WarmHttpsWireTest and W2WarmUriSchemeWireTest share: launch the TestBed, generate
 * a link, send the app to the background, deliver a URI through startActivity.
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
 * Counts are taken against a baseline read at [launch], so a capture that already holds lines
 * (no CLEAR_LOG) does not satisfy a wait early.
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
    private var registeredOn: Application? = null

    /** Starts the TestBed and waits for its launch to reach the wire: a deeplink and an open. */
    fun launch() {
        baseline = ENDPOINTS.associateWith { posts(it) }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        registeredOn = (app as Application).also { it.registerActivityLifecycleCallbacks(lifecycle) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        awaitPosts(DEEPLINK, OPEN)
        awaitQuiet()
        val launchOpen = openBodies().drop(baseline.getValue(OPEN)).firstOrNull()
        check(launchOpen != null && launchOpen.contains("\"randomized_bundle_token\"")) {
            "no randomized_bundle_token on the launch open; run cold_https first"
        }
    }

    /**
     * Unregisters the lifecycle callbacks launch() added. Without it a finished driver keeps
     * observing the Application for the rest of the process, and a later driver's activity
     * events reach its counters too. Each driver then counts only the stops and destroys
     * between its own launch() and close(). Call it from the test's @After. It does not close the
     * scenario, for the reason in the class comment. Safe to call twice or without a launch.
     */
    fun close() {
        registeredOn?.unregisterActivityLifecycleCallbacks(lifecycle)
        registeredOn = null
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
        check(lifecycle.destroys.get() == 0) {
            "MainActivity was destroyed during delivery, so the link did not reach a living " +
                "activity through onNewIntent"
        }
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
    private fun openBodies(): List<String> {
        val file = captureFile()
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

    /** Follows MainActivity only: whether it is stopped now, and how often it stopped or died. */
    private class MainActivityLifecycle : Application.ActivityLifecycleCallbacks {
        val stops = AtomicInteger()
        val destroys = AtomicInteger()
        @Volatile var stopped = false

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
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }

    private companion object {
        const val DEEPLINK = "/v3/deeplink"
        const val OPEN = "/v3/events/open"
        val ENDPOINTS = listOf(DEEPLINK, OPEN)
        const val CAPTURE_FILE = "branchlogs.txt"
        const val LINK_TIMEOUT_MS = 30_000L
        const val WAIT_MS = 30_000L
        const val POLL_MS = 250L
        const val QUIET_MS = 3_000L
        const val FINAL_QUIET_MS = 12_000L
    }
}
