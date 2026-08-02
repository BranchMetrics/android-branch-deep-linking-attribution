package io.branch.gptdriver.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * HYBRID — Espresso for button clicks, AI for Toast/result validation.
 *
 * Tests Branch event logging (Commerce, Content, Lifecycle, Register View).
 * Each button triggers a BranchEvent that makes a network call and shows
 * a Toast on success or failure.
 *
 * Espresso handles: scrolling and clicking buttons.
 * AI handles: verifying the success Toast appeared (Espresso ToastMatcher is
 *             unreliable on API 30+ where TYPE_TOAST is deprecated).
 */
class EventLoggingHybridTest : BaseGptDriverTest() {

    @Test
    fun sendCommerceEvent_succeeds() {
        // DETERMINISTIC: Scroll to and click "Send Commerce Event"
        onView(withId(R.id.cmdCommerceEvent))
            .perform(scrollTo(), click())

        // AI: Verify the SUCCESS toast appeared (not an error toast)
        // MainActivity shows "✅ Commerce Event Sent: {code}" on success
        // and "❌ Commerce Event Failed: {error}" on failure
        driver.assertCondition(
            "A toast message appeared indicating the Commerce Event was sent successfully. " +
                "The toast should contain 'Commerce Event Sent' or a success indicator. " +
                "It should NOT contain 'Failed' or 'Error'."
        )

        // DETERMINISTIC: Verify main screen is still displayed
        onView(withId(R.id.cmdCommerceEvent))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun sendContentEvent_succeeds() {
        onView(withId(R.id.cmdContentEvent))
            .perform(scrollTo(), click())

        // AI: Verify success toast
        // MainActivity shows "Sent Branch Content Event: {code}" on success
        // and "Error sending Branch Content Event: {error}" on failure
        driver.assertCondition(
            "A toast message appeared indicating the Content Event was sent successfully. " +
                "The toast should contain 'Sent Branch Content Event' or a success indicator. " +
                "It should NOT contain 'Error'."
        )

        onView(withId(R.id.cmdContentEvent))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun sendLifecycleEvent_succeeds() {
        onView(withId(R.id.cmdLifecycleEvent))
            .perform(scrollTo(), click())

        // AI: Verify success toast
        // MainActivity shows "Sent Branch Lifecycle Event: {code}" on success
        // and "Error sending Branch Lifecycle Event: {error}" on failure
        driver.assertCondition(
            "A toast message appeared indicating the Lifecycle Event was sent successfully. " +
                "The toast should contain 'Sent Branch Lifecycle Event' or a success indicator. " +
                "It should NOT contain 'Error'."
        )

        onView(withId(R.id.cmdLifecycleEvent))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun registerView_succeeds() {
        onView(withId(R.id.report_view_btn))
            .perform(scrollTo(), click())

        // AI: Verify the VIEW_ITEM event toast
        // MainActivity shows "VIEW_ITEM event logged" on click
        driver.assertCondition(
            "A toast message appeared confirming the view event was logged. " +
                "The toast should contain 'VIEW_ITEM event logged'."
        )

        onView(withId(R.id.report_view_btn))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }
}
