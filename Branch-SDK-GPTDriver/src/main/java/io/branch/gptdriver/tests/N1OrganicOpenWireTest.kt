package io.branch.gptdriver.tests

import androidx.test.ext.junit.rules.ActivityScenarioRule
import io.branch.branchandroidtestbed.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * N1 organic_open — a launch with no link.
 *
 * Deliberately not a BaseGptDriverTest subclass. That base supplies the AI
 * driver and fails without MOBILEBOOST_API_KEY, and L1 asserts the wire rather
 * than the screen, so it needs neither. What it does need from the base is one
 * line: the activity rule below. Same split the iOS L1 drivers make.
 *
 * Produces no assertion of its own. The capture is the output; the contract
 * that judges it lives in the validator.
 */
class N1OrganicOpenWireTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun organicLaunchEmitsWirePayload() {
        // The rule has already launched MainActivity with no intent data.
        // MainActivity.onCreate calls handleDeepLink(getIntent().getData()),
        // which on an organic launch is null. Whether that still puts a
        // /v3/deeplink on the wire is the open question this capture settles.
        settle()
    }

    private fun settle() {
        Thread.sleep(SETTLE_MS)
    }

    private companion object {
        /** Covers session init and the round trips it triggers. */
        const val SETTLE_MS = 12_000L
    }
}
