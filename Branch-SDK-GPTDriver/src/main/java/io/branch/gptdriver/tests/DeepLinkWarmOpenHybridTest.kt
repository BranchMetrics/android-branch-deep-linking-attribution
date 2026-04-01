package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import io.branch.gptdriver.LinkGenerationIdlingResource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HYBRID — Tests deep link warm open (app already in foreground → receives Branch link).
 *
 * Flow:
 * 1. App is already running (via BaseGptDriverTest's ActivityScenarioRule)
 * 2. Generate a real Branch link
 * 3. Extract the URL
 * 4. Deliver a new ACTION_VIEW intent to the running activity (triggers onNewIntent)
 * 5. Verify "Latest Referring Params" updated with the deep link data
 *
 * This simulates a user clicking a Branch link while the app is already open,
 * which triggers onNewIntent() → Branch.sessionBuilder().reInit().
 */
class DeepLinkWarmOpenHybridTest : BaseGptDriverTest() {

    private var idlingResource: IdlingResource? = null

    companion object {
        private const val TAG = "DeepLinkWarmOpenTest"
    }

    @After
    fun tearDownIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun warmOpen_receivesDeepLinkViaNewIntent() {
        // PHASE 1: Generate a real Branch link while the app is running
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for link generation
        waitForLinkGeneration()

        // Verify link was generated
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: Extract the generated URL
        val extracted = driver.extract(listOf("url_in_text_field"))
        val generatedUrl = extracted["url_in_text_field"]?.toString() ?: ""
        Log.i(TAG, "Generated Branch link for warm open: $generatedUrl")

        assertTrue(
            "Branch link should have been generated, got: '$generatedUrl'",
            generatedUrl.startsWith("https://")
        )

        // PHASE 2: Deliver a deep link intent to the running activity (warm open)
        // This triggers onNewIntent() → Branch.sessionBuilder().reInit()
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        activityRule.scenario.onActivity { activity ->
            activity.onNewIntent(deepLinkIntent)
        }

        // Wait for Branch to process the deep link via reInit
        Thread.sleep(5000)

        // PHASE 3: Verify deep link was received via "Latest Referring Params"
        onView(withId(R.id.cmdPrintLatestParam)).perform(click())

        // DETERMINISTIC: Verify dialog appeared
        onView(withText("Latest Referring Params"))
            .check(matches(isDisplayed()))

        // AI: Validate the referring params contain deep link data
        driver.assertBulk(
            listOf(
                "An alert dialog is visible showing JSON text",
                "The JSON contains '+clicked_branch_link' with value 'true' " +
                    "or the JSON contains a '~' prefixed key (like '~channel' or '~feature'), " +
                    "indicating the deep link was received and parsed by the Branch SDK"
            )
        )

        // AI: Extract JSON to verify programmatically
        val jsonExtracted = driver.extract(listOf("json_content_in_dialog"))
        val jsonContent = jsonExtracted["json_content_in_dialog"]?.toString() ?: ""
        Log.i(TAG, "Latest Referring Params JSON: $jsonContent")

        assertTrue(
            "Referring params should not be empty after warm open, got: '$jsonContent'",
            jsonContent.isNotEmpty()
        )

        // DETERMINISTIC: Dismiss dialog
        onView(withText("OK")).perform(click())

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
