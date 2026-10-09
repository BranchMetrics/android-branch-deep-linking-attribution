package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
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

/**
 * The steps W1WarmHttpsWireTest and W2WarmUriSchemeWireTest share: launch the TestBed, generate
 * a link, send the app to the background, deliver a URI through startActivity.
 *
 * Each step waits for something it can observe: the TestBed's own capture file
 * (branchlogs.txt, read in this process, which is the app's), the short-URL field, or the
 * activity's lifecycle state. After each step the capture must also stay unchanged for
 * [QUIET_MS]. That window is the one place a sleep stands in for a condition: it waits for
 * requests that must not be there (a second open, a stray event), and no signal says they will
 * not come. It is what lets the contract's exact counts see an extra request.
 *
 * Counts are taken against a baseline read at [launch], so a capture that already holds lines
 * (no CLEAR_LOG) does not satisfy a wait early.
 *
 * The scenario is never closed here, and the drivers use no ActivityScenarioRule, for the same
 * reason: once a new intent has arrived through startActivity the scenario has lost lifecycle
 * control, so close() throws "Activity never becomes DESTROYED". DeepLinkWarmOpenHybridTest hit
 * the same wall. The runner cleans the activity up.
 */
internal class WarmScenarioDriver {

    private var scenario: ActivityScenario<MainActivity>? = null
    private var baseline = emptyMap<String, Int>()

    /** Starts the TestBed and waits for its launch to reach the wire: a deeplink and an open. */
    fun launch() {
        baseline = ENDPOINTS.associateWith { posts(it) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        awaitPosts(DEEPLINK, OPEN)
        awaitQuiet()
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

    /** Presses Home and waits until the activity has stopped, the state a warm link needs. */
    fun background() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
        // A stopped activity reports CREATED.
        await("the activity did not stop after Home") { scenario?.state == Lifecycle.State.CREATED }
        awaitQuiet()
    }

    /**
     * Delivers [uri] and waits for the deeplink and open it causes, then for quiet. A request
     * that never comes is not an error here: the contract that judges the capture names what
     * is missing, and stopping this driver early would skip that judgement.
     */
    fun deliver(uri: String) {
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
        awaitQuiet()
    }

    private fun captureFile() =
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, CAPTURE_FILE)

    /** How many requests to [endpoint] the capture holds. */
    private fun posts(endpoint: String): Int {
        val file = captureFile()
        if (!file.exists()) return 0
        return file.readLines().count { it.contains("posting to ") && it.trim().endsWith(endpoint) }
    }

    private fun arrived(endpoint: String) = posts(endpoint) > baseline.getValue(endpoint)

    private fun awaitPosts(vararg endpoints: String) =
        await("no ${endpoints.joinToString(" and ")} in the capture") { endpoints.all { arrived(it) } }

    /** Returns once the capture has not changed for [QUIET_MS]. */
    private fun awaitQuiet() {
        var size = captureFile().length()
        var since = System.currentTimeMillis()
        await("the capture was still changing after ${WAIT_MS}ms") {
            val now = captureFile().length()
            if (now != size) {
                size = now
                since = System.currentTimeMillis()
            }
            System.currentTimeMillis() - since >= QUIET_MS
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

    private companion object {
        const val DEEPLINK = "/v3/deeplink"
        const val OPEN = "/v3/events/open"
        val ENDPOINTS = listOf(DEEPLINK, OPEN)
        const val CAPTURE_FILE = "branchlogs.txt"
        const val LINK_TIMEOUT_MS = 30_000L
        const val WAIT_MS = 30_000L
        const val POLL_MS = 250L
        const val QUIET_MS = 3_000L
    }
}
