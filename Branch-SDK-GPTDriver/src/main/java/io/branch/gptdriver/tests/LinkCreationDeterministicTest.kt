package io.branch.gptdriver.tests

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import androidx.test.espresso.IdlingResource
import io.branch.gptdriver.LinkGenerationIdlingResource
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DETERMINISTIC approach — 100% Espresso.
 *
 * All actions and assertions use resource IDs and Espresso matchers.
 *
 * Best for: fixed UI elements, known resource IDs, predictable flows.
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
        // Tap "Create Branch Link" by resource ID
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for async link generation using IdlingResource
        waitForLinkGeneration()

        // Assert URL field contains the expected Branch domain
        // TestBed uses test mode → domain is bnctestbed.test-app.link
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("bnctestbed")))
    }

    @Test
    fun createBranchLink_urlStartsWithHttps() {
        // Tap button by resource ID
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for async link generation using IdlingResource
        waitForLinkGeneration()

        // Assert URL starts with https
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))
    }

    private fun waitForLinkGeneration() {
        activityRule.scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = LinkGenerationIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }
    }
}
