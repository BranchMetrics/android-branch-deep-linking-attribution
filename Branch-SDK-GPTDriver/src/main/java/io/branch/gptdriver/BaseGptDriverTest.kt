package io.branch.gptdriver

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.branch.branchandroidtestbed.MainActivity
import io.branch.branchandroidtestbed.R
import io.mobileboost.gptdriver_lib.GptDriver
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for GPTDriver E2E tests against the Branch SDK TestBed.
 *
 * Uses gptdriver-lib for View/XML-based apps.
 * Docs: https://docs.mobileboost.io/gpt-driver-sdk/espresso/view-xml-based-apps/example
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class BaseGptDriverTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    protected lateinit var driver: GptDriver

    companion object {
        private const val TAG = "GPTDriverTest"
    }

    @Before
    open fun setUp() {
        // Espresso's default 60s master-policy and idling-resource timeouts
        // are too aggressive for this suite:
        //   - LinkGenerationIdlingResource waits for the Branch backend to
        //     round-trip a link, which is slower on emulators with flaky
        //     network.
        //   - `driver.extract` and other AI calls can take 20-40s each; if
        //     two fire back-to-back, the main looper may not idle within 60s.
        // Bumping both to 2 minutes gives the real work room to finish
        // without masking genuine hangs.
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
        Log.i(TAG, "GptDriver initialized")
    }

    @After
    open fun tearDown() {
        // Session status should be set by the individual tests upon successful completion.
        // Setting it here would mark failed tests as "success" — a false positive.
    }

    /**
     * Assert the test is back on the TestBed MainActivity. Anchors on a
     * stable button visible on the main layout so individual tests don't
     * hardcode view IDs that may change. If the anchor moves, update only
     * this method.
     */
    protected fun assertOnMainScreen() {
        onView(withId(R.id.openInAppBrowser)).check(matches(isDisplayed()))
    }
}
