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
 * HYBRID — Tests deep link warm open (app already running → receives Branch link).
 *
 * Simulates:  App is in foreground → user taps a Branch link → system delivers
 *             new intent → onNewIntent() calls Branch.sessionBuilder().reInit()
 *
 * Flow:
 * 1. App is already running (via BaseGptDriverTest's ActivityScenarioRule)
 * 2. Generate a real Branch link
 * 3. Extract the URL via AI
 * 4. Deliver a new intent to the activity (same as system calling onNewIntent)
 * 5. Verify "Latest Referring Params" updated with the deep link metadata
 *
 * Note: We call onNewIntent() directly. MainActivity.onNewIntent() does:
 *   this.setIntent(intent)
 *   Branch.sessionBuilder(this).withCallback(...).reInit()
 * This is the exact production code path for warm opens.
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
        // PHASE 1: Generate a real Branch link while app is running
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        waitForLinkGeneration()

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

        // PHASE 2: Deliver deep link intent to running activity
        // FLAG_ACTIVITY_SINGLE_TOP matches the singleTask launchMode behavior
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // Directly invoke onNewIntent — this is the production code path:
        // MainActivity.onNewIntent() → setIntent() → Branch.sessionBuilder().reInit()
        activityRule.scenario.onActivity { activity ->
            activity.onNewIntent(deepLinkIntent)
        }

        // Wait for Branch reInit to resolve the deep link
        Thread.sleep(5000)

        // PHASE 3: Verify deep link data via "Latest Referring Params"
        onView(withId(R.id.cmdPrintLatestParam)).perform(click())

        onView(withText("Latest Referring Params"))
            .check(matches(isDisplayed()))

        // AI: Validate the JSON contains link metadata from the deep link
        // The generated link has channel="Sharing_Channel_name", feature="my_feature_name"
        driver.assertCondition(
            "An alert dialog is showing JSON text. The JSON should contain " +
                "link metadata keys such as '~channel', '~feature', '$canonical_identifier', " +
                "'~creation_source', or '+match_guaranteed'. " +
                "Any of these keys being present proves the deep link was resolved by the SDK."
        )

        // AI: Extract JSON for programmatic validation
        val jsonExtracted = driver.extract(listOf("json_content_in_dialog"))
        val jsonContent = jsonExtracted["json_content_in_dialog"]?.toString() ?: ""
        Log.i(TAG, "Warm open referring params: $jsonContent")

        assertTrue(
            "Referring params should not be empty after warm open, got: '$jsonContent'",
            jsonContent.isNotEmpty()
        )
        assertTrue(
            "Referring params should contain JSON object, got: '$jsonContent'",
            jsonContent.trimStart().startsWith("{")
        )

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
