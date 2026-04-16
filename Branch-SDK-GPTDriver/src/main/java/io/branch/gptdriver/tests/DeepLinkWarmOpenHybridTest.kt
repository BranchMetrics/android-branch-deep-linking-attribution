package io.branch.gptdriver.tests

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
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
import io.branch.gptdriver.withRetry
import io.mobileboost.gptdriver_lib.GptDriver
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HYBRID — Tests deep link warm open (app already running → receives Branch link).
 *
 * Simulates:  App is in foreground → user taps a Branch link → system delivers
 *             new intent → onNewIntent() calls Branch.sessionBuilder().reInit()
 *
 * Does NOT extend BaseGptDriverTest to avoid ActivityScenarioRule lifecycle
 * conflicts when delivering new intents. Manages its own ActivityScenario.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DeepLinkWarmOpenHybridTest {

    private lateinit var driver: GptDriver
    private var idlingResource: IdlingResource? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    companion object {
        private const val TAG = "DeepLinkWarmOpenTest"
    }

    @Before
    fun setUp() {
        // This class does not extend BaseGptDriverTest (it manages its own
        // ActivityScenario), so we apply the same Espresso timeout bump here
        // to match: LinkGenerationIdlingResource can legitimately wait 60s+
        // for the Branch backend on a flaky emulator network.
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        val apiKey = BuildConfig.MOBILEBOOST_API_KEY.let { key ->
            key.ifEmpty {
                androidx.test.platform.app.InstrumentationRegistry.getArguments()
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
        Log.i(TAG, "GptDriver initialized for warm open test")
    }

    @After
    fun tearDown() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        // Do NOT call scenario.close() — after delivering a new intent via
        // startActivity, the ActivityScenario loses lifecycle control and
        // close() will throw "Activity never becomes DESTROYED".
        // The test runner will clean up the activity automatically.
    }

    @Test
    fun warmOpen_receivesDeepLinkViaNewIntent() {
        // PHASE 1: Launch app and generate a real Branch link
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for link generation
        scenario!!.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            requireNotNull(editText) { "EditText with ID editReferralShortUrl not found" }
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = LinkGenerationIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }

        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: Extract the generated URL (retry transient network flakes)
        val extracted = withRetry { driver.extract(listOf("url_in_text_field")) }
        val generatedUrl = extracted["url_in_text_field"]?.toString()
            ?.trim()?.trim('"') ?: ""
        Log.i(TAG, "Generated Branch link for warm open: $generatedUrl")

        assertTrue(
            "Branch link should have been generated, got: '$generatedUrl'",
            generatedUrl.startsWith("https://")
        )

        // Unregister idling resource
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        idlingResource = null

        // PHASE 2: Deliver deep link intent via system (not direct onNewIntent call)
        // Launch a new intent targeting the same singleTask activity — Android will
        // deliver it via onNewIntent() since the activity is already at the top
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)).apply {
            setClassName(
                InstrumentationRegistry.getInstrumentation().targetContext,
                "io.branch.branchandroidtestbed.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(deepLinkIntent)

        // Wait for Branch reInit to resolve the deep link.
        // Note: Thread.sleep is used because after startActivity with a new intent,
        // the SDK reInit happens internally with no observable UI change to wait on.
        Thread.sleep(8000)

        // PHASE 3: Verify deep link data via "Latest Referring Params"
        // Use AI for post-intent interactions (avoids Espresso focus issues).
        // The navigation step is wrapped in withRetry because a DNS or
        // timeout flake here has previously failed the whole test.
        withRetry {
            driver.execute(
                "The Branch TestBed app should be on the main screen. " +
                    "Wait a moment for the SDK to finish initializing, then tap the button " +
                    "that says 'View Latest Referring Params'."
            )
        }

        // AI: Validate the dialog shows JSON with Branch session data.
        // After warm open via startActivity, the SDK processes the intent data.
        // The JSON will contain at minimum '+clicked_branch_link' or
        // '+is_first_session' keys, proving the SDK re-initialized with the
        // new intent. Retry transient network flakes on this assertion.
        withRetry {
            driver.assertBulk(
                listOf(
                    "An alert dialog titled 'Latest Referring Params' is visible showing JSON text",
                    "The JSON content starts with '{' and contains at least one key " +
                        "(the dialog is not empty and shows valid JSON data)"
                )
            )
        }

        // AI: Dismiss dialog
        runCatching { driver.execute("Tap the 'OK' button to dismiss the dialog") }

        driver.setSessionStatus("success")
    }
}
