package io.branch.gptdriver.tests

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import androidx.test.espresso.IdlingResource
import io.branch.gptdriver.LinkGenerationIdlingResource
import org.junit.After
import org.junit.Test

/**
 * DETERMINISTIC approach — 100% Espresso.
 *
 * All actions and assertions use resource IDs and Espresso matchers.
 * GPTDriver is only used for session status reporting.
 *
 * Best for: fixed UI elements, known resource IDs, predictable flows.
 */
class LinkCreationDeterministicTest : BaseGptDriverTest() {

    private var idlingResource: IdlingResource? = null

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

        driver.setSessionStatus("success")
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

        driver.setSessionStatus("success")
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
