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

        // AI: Validate the share sheet appeared with sharing options
        driver.assertBulk(
            listOf(
                "A share sheet or sharing dialog is visible on screen",
                "The share sheet shows sharing options or app icons for sharing"
            )
        )

        // AI: Dismiss the share sheet to return to the main screen
        driver.execute(
            "Dismiss the share sheet by pressing the back button or tapping outside it. " +
                "Return to the main screen of the Branch TestBed app."
        )

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
            "A native Android share chooser or 'Share with' dialog is visible. " +
                "It should show a list of apps or sharing options available on the device."
        )

        // AI: Dismiss and return to main screen
        driver.execute(
            "Dismiss the share chooser by pressing the back button or tapping outside. " +
                "Return to the main screen."
        )

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
        val shareInfo = driver.extract(
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
