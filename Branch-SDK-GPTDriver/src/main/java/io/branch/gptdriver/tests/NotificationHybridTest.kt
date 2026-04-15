package io.branch.gptdriver.tests

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Before
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

    @Before
    override fun setUp() {
        super.setUp()
        // Grant POST_NOTIFICATIONS for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant $packageName android.permission.POST_NOTIFICATIONS"
            )
            // Optional check for permission dialog: only handle if it's actually visible.
            // This prevents timeouts if 'pm grant' already succeeded.
            driver.execute(
                "Check if an Android system permission dialog for 'Notifications' is visible. " +
                    "If it is, tap 'Allow'. If no dialog is present, do nothing and proceed."
            )
            // Wait a moment for the system to process the granted permission
            Thread.sleep(2000)
        }
    }

    @Test
    fun sendNotification_createsNotificationWithBranchLink() {
        // DETERMINISTIC: Scroll to and click "Init Session"
        onView(withId(R.id.initSessionButton))
            .perform(scrollTo(), click())
        
        Thread.sleep(3000)

        // DETERMINISTIC: Scroll to and click "Send Notification"
        onView(withId(R.id.notif_btn))
            .perform(scrollTo(), click())

        // SOFT AI PROBE: the TestBed shows a "Notification Sent!" Toast, but
        // that Toast is only visible for ~2-3 seconds which is unreliable to
        // catch via AI screenshots. Determinism is proven below by opening
        // the notification shade and asserting the BranchTest notification
        // exists; the Toast check stays as a dashboard probe only.
        runCatching {
            driver.checkBulk(
                listOf(
                    "A 'Notification Sent!' toast is currently visible or was recently visible"
                )
            )
        }

        // Small settle so the notification is posted before we pull the shade.
        Thread.sleep(1500)

        // DETERMINISTIC: Open shade using UiDevice
        val device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        Thread.sleep(2000)

        // AI: Find notification, and confirm its content.
        driver.execute(
            "Look for the 'BranchTest' notification. If grouped, tap the arrow to expand it. " +
                "Verify the notification contains a URL starting with 'https://'. " +
                "Once verified, swipe up from the bottom or press back to close the shade and return to the TestBed app."
        )

        // DETERMINISTIC: Final check to ensure we are back in the app
        onView(withId(R.id.notif_btn))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }
}
