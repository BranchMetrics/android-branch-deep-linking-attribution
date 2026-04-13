package io.branch.gptdriver.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * HYBRID — Espresso for button clicks, AI for system share sheet validation.
 *
 * Tests Branch share functionality. Both "Share Branch Link" and "Native Share"
 * open system-level share sheets that Espresso cannot interact with directly.
 * AI is required to validate the share sheet appeared and dismiss it.
 */
class ShareLinkHybridTest : BaseGptDriverTest() {

    @Test
    fun shareBranchLink_opensShareSheet() {
        // DETERMINISTIC: Click "Share Branch Link"
        onView(withId(R.id.share_btn)).perform(click())

        // AI: Validate the custom share dialog appeared
        driver.assertBulk(
            listOf(
                "A share dialog or list of apps is visible on screen",
                "The dialog contains a list of sharing options or app icons"
            )
        )

        // AI: Dismiss the custom dialog to return to the main screen
        val device = androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        
        // Wait for transition
        Thread.sleep(3000)

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.share_btn))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun nativeShareBranchLink_opensNativeChooser() {
        // DETERMINISTIC: Click "Native Share Branch Link"
        onView(withId(R.id.native_share_btn)).perform(click())

        // AI: Validate the native Android share chooser appeared
        driver.assertCondition(
            "A native Android system share chooser or 'Share with' dialog is visible. " +
                "It should show a list of apps or sharing options provided by the system."
        )

        // AI: Dismiss and return to main screen
        val device = androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        device.pressBack()

        // Wait for system transition back to the app
        Thread.sleep(4000)

        // DETERMINISTIC: Verify we're back
        onView(withId(R.id.native_share_btn))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun shareBranchLink_containsShareContent() {
        // DETERMINISTIC: Click "Share Branch Link"
        onView(withId(R.id.share_btn)).perform(click())

        // AI: Extract visible sharing details from the share sheet
        driver.extract(
            listOf("share_title_or_subject", "share_message_or_content")
        )

        // AI: Verify share content is not empty
        driver.assertCondition(
            "The share sheet is visible and contains sharing content — " +
                "either a message, subject, or Branch link URL is displayed."
        )

        // AI: Dismiss
        driver.execute("Press back to dismiss the share sheet and return to main screen")

        driver.setSessionStatus("success")
    }
}
