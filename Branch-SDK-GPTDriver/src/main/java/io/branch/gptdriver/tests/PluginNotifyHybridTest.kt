package io.branch.gptdriver.tests

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import io.branch.gptdriver.LinkGenerationIdlingResource
import org.junit.After
import org.junit.Test

/**
 * HYBRID — Espresso for actions, AI not needed here but IdlingResource for async.
 *
 * Tests "Simulate Plugin Notify Init" (Branch.notifyNativeToInit()).
 * This API is used by plugins (Flutter, React Native) to signal native SDK init.
 *
 * Since notifyNativeToInit() produces no visible feedback, we verify the SDK
 * is still functional after the call by creating a Branch link.
 */
class PluginNotifyHybridTest : BaseGptDriverTest() {

    private var idlingResource: IdlingResource? = null

    @After
    fun tearDownIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun pluginNotifyInit_sdkRemainsFunction() {
        // DETERMINISTIC: Scroll to and click "Simulate Plugin Notify Init"
        onView(withId(R.id.notifyInit_btn))
            .perform(scrollTo(), click())

        // Allow time for the native init callback to process.
        // Note: Thread.sleep is used here because notifyNativeToInit() has no observable
        // state change to wait on — it triggers an internal SDK init with no UI feedback.
        Thread.sleep(2000)

        // Verify SDK is still alive by creating a Branch link
        onView(withId(R.id.cmdRefreshShortURL))
            .perform(scrollTo(), click())

        // Wait for link generation
        waitForLinkGeneration()

        // DETERMINISTIC: Assert a valid URL was generated
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("bnctestbed")))

        driver.setSessionStatus("success")
    }

    private fun waitForLinkGeneration() {
        activityRule.scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.editReferralShortUrl)
            requireNotNull(editText) { "EditText with ID editReferralShortUrl not found" }
            idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
            idlingResource = LinkGenerationIdlingResource(editText).also {
                IdlingRegistry.getInstance().register(it)
            }
        }
    }
}
