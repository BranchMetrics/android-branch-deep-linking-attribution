package io.branch.gptdriver.tests

import android.os.Bundle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import io.branch.referral.PrefHelper
import io.branch.referral.util.LinkProperties
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Generates the link a cold scenario delivers and reports it as the instrumentation status
 * `l1_link_url`, so the harness can deliver it from the host into a stopped app.
 *
 * Arguments: `L1_SCENARIO` (required) and `L1_RUN_ID`. Both go into the link data, so each
 * scenario resolves a link of its own. `L1_ATTRIBUTION_LEVEL` sets that level once the link exists.
 */
class ScenarioLinkGenerator {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun generateScenarioLink() {
        val args = InstrumentationRegistry.getArguments()
        val scenario = requireNotNull(args.getString(ARG_SCENARIO)) { "$ARG_SCENARIO is required" }
        val runId = args.getString(ARG_RUN_ID) ?: System.currentTimeMillis().toString()
        val level = args.getString(ARG_LEVEL)?.let { Defines.BranchAttributionLevel.valueOf(it) }

        Thread.sleep(SESSION_MS)
        if (level != null) {
            check(awaitTokens()) { "No randomized tokens within ${TOKEN_MS}ms; the install open did not complete" }
        }

        val latch = CountDownLatch(1)
        var url: String? = null
        var error: BranchError? = null
        val properties = LinkProperties()
            .addControlParameter(KEY_SCENARIO, scenario)
            .addControlParameter(KEY_RUN_ID, runId)
        activityRule.scenario.onActivity { activity ->
            BranchUniversalObject()
                .setCanonicalIdentifier("l1/$scenario/$runId")
                .generateShortUrl(activity, properties, { generated, failure ->
                    url = generated
                    error = failure
                    latch.countDown()
                }, false)
        }

        check(latch.await(LINK_MS, TimeUnit.MILLISECONDS)) { "No link within ${LINK_MS}ms" }
        check(error == null) { "Link generation failed: ${error?.message}" }
        val link = url.orEmpty()
        check(link.startsWith("https://")) { "Expected an https link, got '$link'" }

        if (level != null) {
            // Safe to await: NONE calls back synchronously; another level would wait for an init request.
            val levelSet = CountDownLatch(1)
            activityRule.scenario.onActivity {
                Branch.getInstance().setConsumerProtectionAttributionLevel(level) { _, _, _ -> levelSet.countDown() }
            }
            check(levelSet.await(LINK_MS, TimeUnit.MILLISECONDS)) { "No level callback within ${LINK_MS}ms" }
            check(hasTokens()) { "Randomized tokens cleared by setting $level" }
        }

        InstrumentationRegistry.getInstrumentation()
            .sendStatus(STATUS_CODE, Bundle().apply { putString(STATUS_KEY, link) })
    }

    private fun hasTokens(): Boolean {
        val prefs = PrefHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
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

    private companion object {
        const val ARG_SCENARIO = "L1_SCENARIO"
        const val ARG_RUN_ID = "L1_RUN_ID"
        const val ARG_LEVEL = "L1_ATTRIBUTION_LEVEL"
        const val KEY_SCENARIO = "l1_scenario"
        const val KEY_RUN_ID = "l1_run_id"
        const val STATUS_KEY = "l1_link_url"
        const val STATUS_CODE = 2
        const val SESSION_MS = 6_000L
        const val LINK_MS = 15_000L
        const val TOKEN_MS = 20_000L
        const val TOKEN_POLL_MS = 250L
    }
}
