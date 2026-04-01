package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BuildConfig
import io.branch.gptdriver.LinkGenerationIdlingResource
import io.mobileboost.gptdriver_lib.GptDriver
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HYBRID — Tests deep link cold open (app not running → launched via Branch link).
 *
 * Flow:
 * 1. Launch the app normally to generate a real Branch link
 * 2. Extract the URL
 * 3. Close the app completely
 * 4. Relaunch with an ACTION_VIEW intent containing the Branch link
 * 5. Verify "Latest Referring Params" contains the deep link data
 *
 * This does NOT extend BaseGptDriverTest because we need to control
 * the activity lifecycle manually (close and relaunch with a deep link intent).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DeepLinkColdOpenHybridTest {

    private lateinit var driver: GptDriver
    private var idlingResource: IdlingResource? = null

    companion object {
        private const val TAG = "DeepLinkColdOpenTest"
    }

    @Before
    fun setUp() {
        val apiKey = BuildConfig.MOBILEBOOST_API_KEY.let { key ->
            key.ifEmpty { System.getenv("GPTDRIVER_API_KEY") ?: "" }
        }
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "MOBILEBOOST_API_KEY must be set in local.properties, " +
                    "gradle property (-PMOBILEBOOST_API_KEY=xxx), " +
                    "or env var GPTDRIVER_API_KEY"
            )
        }
        driver = GptDriver(apiKey)
        Log.i(TAG, "GptDriver initialized for cold open test")
    }

    @After
    fun tearDown() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun coldOpen_receivesDeepLinkParams() {
        // PHASE 1: Launch app normally and generate a Branch link
        val normalLaunch = ActivityScenario.launch(MainActivity::class.java)

        // Click "Create Branch Link" to generate a URL
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for link generation using IdlingResource
        normalLaunch.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = LinkGenerationIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }

        // Verify link was generated
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: Extract the generated URL for use in deep link test
        val extracted = driver.extract(listOf("url_in_text_field"))
        val generatedUrl = extracted["url_in_text_field"]?.toString() ?: ""
        Log.i(TAG, "Generated Branch link: $generatedUrl")

        assertTrue(
            "Branch link should have been generated, got: '$generatedUrl'",
            generatedUrl.startsWith("https://")
        )

        // Unregister idling resource before closing
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        idlingResource = null

        // Close the app completely
        normalLaunch.close()

        // PHASE 2: Relaunch app with a deep link intent (simulates cold open)
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)).apply {
            setClassName(
                InstrumentationRegistry.getInstrumentation().targetContext,
                "io.branch.branchandroidtestbed.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val deepLinkLaunch = ActivityScenario.launch<MainActivity>(deepLinkIntent)

        // Wait for Branch session init to process the deep link
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

        // DETERMINISTIC: Dismiss dialog
        onView(withText("OK")).perform(click())

        deepLinkLaunch.close()

        driver.setSessionStatus("success")
    }
}
