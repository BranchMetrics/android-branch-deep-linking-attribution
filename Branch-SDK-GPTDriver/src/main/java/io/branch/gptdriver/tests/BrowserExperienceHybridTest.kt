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
 * HYBRID — Espresso for button click, AI for WebView/browser validation.
 *
 * Tests the in-app browser experience. The "Browser Test" button calls
 * Branch.openBrowserExperience() which opens an in-app WebView directed
 * to https://branch.io.
 *
 * Espresso handles: scrolling and clicking the button.
 * AI handles: validating the WebView opened, content loaded, and navigation back.
 */
class BrowserExperienceHybridTest : BaseGptDriverTest() {

    @Test
    fun browserTest_opensInAppWebView() {
        // DETERMINISTIC: Scroll to and click "Browser Test"
        onView(withId(R.id.openInAppBrowser))
            .perform(scrollTo(), click())

        // AI: Validate a WebView or browser opened
        driver.assertCondition(
            "An in-app browser or WebView has opened. " +
                "It should show web content (a webpage loading or loaded), " +
                "not the main TestBed screen."
        )

        // AI: Navigate back to the main screen
        driver.execute(
            "Press the back button or close the in-app browser to return to the " +
                "main screen of the Branch TestBed app."
        )

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.openInAppBrowser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun browserTest_loadsWebContent() {
        // DETERMINISTIC: Scroll to and click "Browser Test"
        onView(withId(R.id.openInAppBrowser))
            .perform(scrollTo(), click())

        // AI: Validate that actual web content loaded (not blank/error)
        driver.assertBulk(
            listOf(
                "A browser or WebView is visible on screen",
                "The page has loaded with visible content (text, images, or UI elements) — " +
                    "it is not showing a blank page or an error message"
            )
        )

        // AI: Go back
        driver.execute("Press back to return to the main TestBed screen")

        driver.setSessionStatus("success")
    }
}
