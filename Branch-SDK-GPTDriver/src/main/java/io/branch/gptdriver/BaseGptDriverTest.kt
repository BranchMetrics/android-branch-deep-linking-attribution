package io.branch.gptdriver

import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.branch.branchandroidtestbed.MainActivity
import io.mobileboost.gptdriver_lib.GptDriver
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
        Log.i(TAG, "GptDriver initialized")
    }

    @After
    open fun tearDown() {
        if (::driver.isInitialized) {
            try {
                driver.setSessionStatus("success")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set session status", e)
            }
        }
    }
}
