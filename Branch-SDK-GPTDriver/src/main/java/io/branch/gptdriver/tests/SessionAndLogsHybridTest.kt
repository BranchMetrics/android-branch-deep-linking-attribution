package io.branch.gptdriver.tests

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import io.branch.gptdriver.LinkGenerationIdlingResource
import org.junit.After
import org.junit.Test

/**
 * HYBRID — Espresso for actions, AI for result validation.
 *
 * Tests session initialization, log viewing, and logout.
 *
 * - initSession: verified by creating a link after init (proves session is alive)
 * - loadBranchLogs: verified by navigating to LogOutputActivity and checking content
 * - logout: verified by AI Toast assertion (not swallowed try/catch)
 */
class SessionAndLogsHybridTest : BaseGptDriverTest() {

    private var idlingResource: IdlingResource? = null

    @After
    fun tearDownIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun initSession_sessionIsAlive() {
        // DETERMINISTIC: Click "Init Session" to re-initialize
        onView(withId(R.id.initSessionButton))
            .perform(scrollTo(), click())

        // Verify session is alive by creating a Branch link
        // If session failed, link creation would return an error
        onView(withId(R.id.cmdRefreshShortURL))
            .perform(scrollTo(), click())

        // Wait for link generation
        waitForLinkGeneration()

        // AI: Verify a valid URL was generated (proves session is functional)
        driver.assertCondition(
            "The text field at the top of the screen contains a valid Branch URL " +
                "starting with 'https://' and containing 'bnctestbed'. " +
                "This proves the session was initialized successfully."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun loadBranchLogs_showsLogContent() {
        // DETERMINISTIC: Navigate to log screen
        onView(withId(R.id.viewLogsButton))
            .perform(scrollTo(), click())

        // DETERMINISTIC: Verify we're on the LogOutputActivity
        onView(withId(R.id.logOutputTextView))
            .check(matches(isDisplayed()))

        // AI: Verify the log screen has actual content (not empty/placeholder)
        driver.assertCondition(
            "The log screen is displayed and contains log text entries. " +
                "The text should contain Branch SDK log entries (timestamps, messages), " +
                "not just the placeholder 'Logs will appear here' or 'Log file not found'."
        )

        // DETERMINISTIC: Navigate back
        pressBack()

        // DETERMINISTIC: Verify we're back on main screen
        onView(withId(R.id.viewLogsButton))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun logout_showsConfirmation() {
        // DETERMINISTIC: Scroll to and click "Simulate Logout"
        onView(withId(R.id.logout_btn))
            .perform(scrollTo(), click())

        // AI: Verify the logout Toast appeared (not swallowed in try/catch)
        driver.assertCondition(
            "A toast message appeared confirming logout. " +
                "The toast should contain 'Logged Out'."
        )

        // DETERMINISTIC: Verify main screen is still displayed
        onView(withId(R.id.logout_btn))
            .check(matches(isDisplayed()))

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
