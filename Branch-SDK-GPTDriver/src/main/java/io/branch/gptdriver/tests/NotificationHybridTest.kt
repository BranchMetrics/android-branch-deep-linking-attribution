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
 * HYBRID — Espresso for button click, AI for notification verification.
 *
 * Tests the "Send Notification" feature. The button generates a short URL
 * synchronously, then creates an Android notification with the URL as content.
 * The notification includes a PendingIntent that re-opens MainActivity.
 *
 * Espresso handles: scrolling and clicking the button.
 * AI handles: opening notification shade, verifying notification content.
 */
class NotificationHybridTest : BaseGptDriverTest() {

    @Test
    fun sendNotification_createsNotificationWithBranchLink() {
        // DETERMINISTIC: Scroll to and click "Send Notification"
        onView(withId(R.id.notif_btn))
            .perform(scrollTo(), click())

        // Wait for sync link generation + notification creation
        Thread.sleep(3000)

        // AI: Open the notification shade and verify the notification
        driver.execute(
            "Swipe down from the top of the screen to open the notification shade. " +
                "Look for a notification from 'BranchTest' or the TestBed app."
        )

        // AI: Validate the notification content
        driver.assertBulk(
            listOf(
                "A notification is visible in the notification shade",
                "The notification title contains 'BranchTest'",
                "The notification content contains a URL with 'https://' or a Branch link"
            )
        )

        // AI: Dismiss the notification shade and return to the app
        driver.execute(
            "Press the back button or swipe up to close the notification shade " +
                "and return to the Branch TestBed app."
        )

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.notif_btn))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }
}
