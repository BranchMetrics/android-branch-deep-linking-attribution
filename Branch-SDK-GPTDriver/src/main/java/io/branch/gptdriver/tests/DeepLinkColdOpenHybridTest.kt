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
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
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
 * HYBRID — Tests deep link cold open (app launched directly via Branch link).
 *
 * Simulates:  User taps a Branch link → app launches with the link as intent data
 *             → Branch SDK processes the link during session init in onStart().
 *
 * Flow:
 * 1. Launch the app normally to generate a real Branch link
 * 2. Extract the URL via AI
 * 3. Close the activity
 * 4. Relaunch with an ACTION_VIEW intent containing the Branch link
 *    (FLAG_ACTIVITY_CLEAR_TASK forces a fresh activity + session init with the URI)
 * 5. Verify "Latest Referring Params" shows link metadata (channel, feature, tags)
 *
 * Note: ActivityScenario.close() does not kill the process, so Branch singleton
 * persists. However, FLAG_ACTIVITY_CLEAR_TASK + new intent data triggers a fresh
 * sessionBuilder().withData(intent.getData()).init() in onStart(), which is the
 * same code path as a real cold open.
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
            key.ifEmpty {
                @Suppress("DEPRECATION")
                androidx.test.InstrumentationRegistry.getArguments()
                    .getString("GPTDRIVER_API_KEY") ?: ""
            }
        }
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "MOBILEBOOST_API_KEY must be set in local.properties, " +
                    "gradle property (-PMOBILEBOOST_API_KEY=xxx), " +
                    "or instrumentation arg GPTDRIVER_API_KEY"
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
        // PHASE 1: Generate a real Branch link
        val normalLaunch = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        normalLaunch.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            requireNotNull(editText) { "EditText with ID editReferralShortUrl not found" }
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = LinkGenerationIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }

        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: Extract the generated URL
        // driver.extract() may return the value wrapped in quotes, so trim them
        val extracted = driver.extract(listOf("url_in_text_field"))
        val generatedUrl = extracted["url_in_text_field"]?.toString()
            ?.trim()?.trim('"') ?: ""
        Log.i(TAG, "Generated Branch link: $generatedUrl")

        assertTrue(
            "Branch link should have been generated, got: '$generatedUrl'",
            generatedUrl.startsWith("https://")
        )

        // Cleanup idling resource before closing
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        idlingResource = null

        normalLaunch.close()

        // PHASE 2: Relaunch with deep link intent
        // CLEAR_TASK forces fresh activity creation → onStart() calls
        // Branch.sessionBuilder().withData(intent.getData()).init()
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)).apply {
            setClassName(
                InstrumentationRegistry.getInstrumentation().targetContext,
                "io.branch.branchandroidtestbed.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val deepLinkLaunch = ActivityScenario.launch<MainActivity>(deepLinkIntent)

        // Wait for Branch session init to resolve the deep link.
        // Note: Thread.sleep is used because after ActivityScenario relaunch, there is no
        // observable UI state to create an IdlingResource for — the SDK init happens
        // internally before any UI update. The AI driver handles the rest.
        Thread.sleep(5000)

        // PHASE 3: Verify deep link data via "Latest Referring Params"
        // After ActivityScenario relaunch, Espresso may lose window focus.
        // Use AI for all post-relaunch interactions to avoid RootViewWithoutFocusException.
        driver.execute(
            "The Branch TestBed app should be on the main screen. " +
                "Tap the button that says 'View Latest Referring Params'."
        )

        // AI: Validate the dialog shows JSON with link metadata
        driver.assertBulk(
            listOf(
                "An alert dialog titled 'Latest Referring Params' is visible showing JSON text",
                "The JSON contains link metadata keys such as '~channel', '~feature', " +
                    "'~creation_source', or '+match_guaranteed' — " +
                    "any of these proves the deep link was resolved by the SDK"
            )
        )

        // AI: Dismiss dialog
        driver.execute("Tap the 'OK' button to dismiss the dialog")

        deepLinkLaunch.close()

        driver.setSessionStatus("success")
    }
}
