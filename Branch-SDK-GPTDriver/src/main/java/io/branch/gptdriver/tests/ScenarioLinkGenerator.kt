package io.branch.gptdriver.tests

import android.os.Bundle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.BranchError
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
 * scenario resolves a link of its own.
 */
class ScenarioLinkGenerator {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun generateScenarioLink() {
        val args = InstrumentationRegistry.getArguments()
        val scenario = requireNotNull(args.getString(ARG_SCENARIO)) { "$ARG_SCENARIO is required" }
        val runId = args.getString(ARG_RUN_ID) ?: System.currentTimeMillis().toString()

        Thread.sleep(SESSION_MS)

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

        InstrumentationRegistry.getInstrumentation()
            .sendStatus(STATUS_CODE, Bundle().apply { putString(STATUS_KEY, link) })
    }

    private companion object {
        const val ARG_SCENARIO = "L1_SCENARIO"
        const val ARG_RUN_ID = "L1_RUN_ID"
        const val KEY_SCENARIO = "l1_scenario"
        const val KEY_RUN_ID = "l1_run_id"
        const val STATUS_KEY = "l1_link_url"
        const val STATUS_CODE = 2
        const val SESSION_MS = 6_000L
        const val LINK_MS = 15_000L
    }
}
