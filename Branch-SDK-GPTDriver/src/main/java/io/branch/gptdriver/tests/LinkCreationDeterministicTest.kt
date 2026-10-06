package io.branch.gptdriver.tests

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deterministic link creation through the TestBed, 100% Espresso.
 *
 * Default TEST_CLASS of scripts/run_l1_instrumented.sh: tapping
 * "Create Branch Link" puts the link request on the wire for the L1 gate,
 * and the assertions confirm the TestBed received a link back.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LinkCreationDeterministicTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private var idlingResource: IdlingResource? = null

    @Before
    fun setUp() {
        // Espresso's default 60s timeouts are too short for the Branch backend
        // to round-trip a link on an emulator with a slow network.
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)
    }

    @After
    fun tearDownIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun createBranchLink_generatesValidUrl() {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        waitForLinkGeneration()

        // TestBed uses test mode, so the domain is bnctestbed.test-app.link
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("bnctestbed")))
    }

    @Test
    fun createBranchLink_urlStartsWithHttps() {
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        waitForLinkGeneration()

        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))
    }

    private fun waitForLinkGeneration() {
        activityRule.scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = UrlPopulatedIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }
    }

    /** Idle once the URL field holds a link (starts with "https://"). */
    private class UrlPopulatedIdlingResource(
        private val editText: EditText
    ) : IdlingResource {

        private var callback: IdlingResource.ResourceCallback? = null

        override fun getName(): String = "UrlPopulatedIdlingResource_${System.identityHashCode(this)}"

        override fun isIdleNow(): Boolean {
            val idle = editText.text?.toString().orEmpty().startsWith("https://")
            if (idle) {
                callback?.onTransitionToIdle()
            }
            return idle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }
    }
}
