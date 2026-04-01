package io.branch.gptdriver.tests

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HYBRID — Espresso for actions, AI for JSON content validation and extraction.
 *
 * Tests "View First Referring Params" and "View Latest Referring Params".
 * Both buttons open an AlertDialog displaying JSON.
 *
 * Espresso handles: button clicks, dialog detection, dialog dismissal.
 * AI handles: validating JSON structure and extracting JSON content for assertions.
 */
class ReferringParamsHybridTest : BaseGptDriverTest() {

    companion object {
        private const val TAG = "ReferringParamsTest"
    }

    @Test
    fun viewFirstReferringParams_showsJsonDialog() {
        // DETERMINISTIC: Click "View First Referring Params"
        onView(withId(R.id.cmdPrintInstallParam)).perform(click())

        // DETERMINISTIC: Verify AlertDialog appeared with title
        onView(withText("First Referring Params"))
            .check(matches(isDisplayed()))

        // AI: Validate dialog shows valid JSON with expected Branch keys
        driver.assertBulk(
            listOf(
                "An alert dialog is visible showing text content",
                "The content starts with '{' indicating JSON format",
                "The JSON contains a '+clicked_branch_link' key"
            )
        )

        // DETERMINISTIC: Dismiss dialog
        onView(withText("OK")).perform(click())

        // Verify we're back on main screen
        onView(withId(R.id.cmdPrintInstallParam))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun viewLatestReferringParams_showsJsonDialog() {
        // DETERMINISTIC: Click "View Latest Referring Params"
        onView(withId(R.id.cmdPrintLatestParam)).perform(click())

        // DETERMINISTIC: Verify AlertDialog appeared with title
        onView(withText("Latest Referring Params"))
            .check(matches(isDisplayed()))

        // AI: Validate dialog shows valid JSON with expected Branch keys
        driver.assertBulk(
            listOf(
                "An alert dialog is visible showing text content",
                "The content starts with '{' indicating JSON format",
                "The JSON contains a '+clicked_branch_link' key"
            )
        )

        // DETERMINISTIC: Dismiss dialog
        onView(withText("OK")).perform(click())

        // Verify we're back on main screen
        onView(withId(R.id.cmdPrintLatestParam))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun viewLatestReferringParams_extractAndValidateJson() {
        // DETERMINISTIC: Click "View Latest Referring Params"
        onView(withId(R.id.cmdPrintLatestParam)).perform(click())

        // DETERMINISTIC: Verify dialog appeared
        onView(withText("Latest Referring Params"))
            .check(matches(isDisplayed()))

        // AI: Extract the JSON content from the dialog for programmatic validation
        val extracted = driver.extract(
            listOf("json_content_in_dialog")
        )
        val jsonContent = extracted["json_content_in_dialog"]?.toString() ?: ""
        Log.i(TAG, "Extracted JSON: $jsonContent")

        // DETERMINISTIC: Validate extracted content
        assertTrue(
            "JSON content should not be empty, got: '$jsonContent'",
            jsonContent.isNotEmpty()
        )
        assertTrue(
            "JSON should start with '{', got: '$jsonContent'",
            jsonContent.trimStart().startsWith("{")
        )

        // DETERMINISTIC: Dismiss dialog
        onView(withText("OK")).perform(click())

        driver.setSessionStatus("success")
    }
}
