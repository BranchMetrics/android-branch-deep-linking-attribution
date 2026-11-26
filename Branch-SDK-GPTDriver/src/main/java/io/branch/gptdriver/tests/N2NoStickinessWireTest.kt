package io.branch.gptdriver.tests

import androidx.test.platform.app.InstrumentationRegistry
import io.branch.referral.Branch
import org.junit.After
import org.junit.Test

/**
 * no_stickiness (N2): an old link must not come back.
 *
 * Two methods, each run in its own instrumentation. scripts/run_l1_instrumented.sh runs them in
 * order with TEST_METHODS and force-stops the app between them, so [relaunchAfterForceStop]
 * starts in a new process.
 *
 * [linkThenReturnFromRecents] opens a marked link warm and checks the app got it. It presses
 * Home, comes back the way the recents screen does (no new intent), and checks the app no longer
 * gets the link: BranchProcessLifecycleObserver.onStop clears it. The return must send nothing,
 * which the contract's counts check. It presses Home again before the force-stop.
 *
 * [relaunchAfterForceStop] opens the app with no link in the new process and checks the app
 * gets no link. The contract checks that its /v3/deeplink and open carry none of the old link,
 * and at NONE that it sends no /v3/deeplink at all: NONE sends a launch's deeplink only when it
 * carries a link, so an old link kept in prefs would make it send one.
 *
 * With L1_ATTRIBUTION_LEVEL=NONE the first method sets NONE after generating the link, so the
 * link and the relaunch happen at NONE, which holds every open. The level stays in prefs, so the
 * run clears app data afterwards (WIPE_AFTER=1).
 *
 * Needs a device that already holds a token, from cold_https running first.
 */
class N2NoStickinessWireTest {

    private val driver = WireScenarioDriver()
    private val none = InstrumentationRegistry.getArguments().getString(ARG_LEVEL) == "NONE"
    private val scenario = if (none) "no_stickiness_none" else "no_stickiness"

    /** Stops this driver observing the Application, so the next test's counters are its own. */
    @After
    fun closeDriver() = driver.close()

    @Test
    fun linkThenReturnFromRecents() {
        driver.launch()
        val link = driver.generateMarkedLink(scenario)
        if (none) driver.setAttributionNone()
        driver.background()
        driver.deliver(link, expectOpen = !none)
        // Without this the checks below would pass on a link that never arrived.
        val delivered = Branch.getInstance().latestReferringParams
        check(delivered.optBoolean(CLICKED) && delivered.optString(MARKER) == scenario) {
            "The app did not get the link: $delivered"
        }
        driver.background()
        driver.returnFromRecents()
        assertNoLink("after the return from recents")
        driver.background()
    }

    @Test
    fun relaunchAfterForceStop() {
        // At NONE a launch with no link sends nothing; a kept old link would make it send one.
        driver.launch(expectRequests = !none)
        assertNoLink("after the relaunch")
    }

    private fun assertNoLink(moment: String) {
        val params = Branch.getInstance().latestReferringParams
        val kept = LINK_KEYS.filter { params.has(it) } + listOfNotNull(CLICKED.takeIf { params.optBoolean(it) })
        check(kept.isEmpty()) { "The old link came back $moment: $kept in $params" }
    }

    private companion object {
        const val ARG_LEVEL = "L1_ATTRIBUTION_LEVEL"
        const val CLICKED = "+clicked_branch_link"
        const val MARKER = "l1_scenario"
        val LINK_KEYS = listOf("~referring_link", "+link_click_id", MARKER)
    }
}
