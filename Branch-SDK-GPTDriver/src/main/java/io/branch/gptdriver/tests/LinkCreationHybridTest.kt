package io.branch.gptdriver.tests

import android.util.Log
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import androidx.test.espresso.IdlingResource
import io.branch.gptdriver.LinkGenerationIdlingResource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HYBRID approach — Espresso for actions, AI for complex validation.
 *
 * Deterministic Espresso handles taps, scrolls, and simple assertions.
 * GPTDriver AI handles visual/contextual checks that Espresso cannot express.
 *
 * Best for: flows with known buttons but dynamic/visual outcomes,
 * or when you need to validate UX behavior beyond text matching.
 */
class LinkCreationHybridTest : BaseGptDriverTest() {

    private var idlingResource: IdlingResource? = null

    @After
    fun tearDownIdlingResource() {
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
    }

    @Test
    fun createBranchLink_fullValidation() {
        // DETERMINISTIC: tap by resource ID (fast, reliable)
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for async link generation using IdlingResource
        waitForLinkGeneration()

        // DETERMINISTIC: assert URL contains expected domain
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("bnctestbed")))

        // DETERMINISTIC: assert URL is HTTPS
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: validate visual aspects Espresso can't check (multiple conditions)
        driver.assertBulk(
            listOf(
                "The generated URL is fully visible in the text field and is not truncated or cut off",
                "The URL text field is not showing an error message",
                "The complete URL is readable and starts with 'https://'"
            )
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun createBranchLink_extractAndValidateUrl() {
        // DETERMINISTIC: tap button
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // Wait for async link generation using IdlingResource
        waitForLinkGeneration()

        // DETERMINISTIC: basic assertion — URL field is populated
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("https://")))

        // AI: extract the actual URL from screen — Espresso can't return text values,
        // only assert against matchers. extract() gives us the raw string to work with.
        val extracted = driver.extract(listOf("url_in_text_field"))
        Log.i("HybridTest", "Extracted raw: $extracted")

        val url = extracted["url_in_text_field"]?.toString() ?: ""
        Log.i("HybridTest", "Extracted URL: $url")

        // DETERMINISTIC: assert on the extracted value with JUnit
        assertTrue("URL should not be empty, got: '$url'", url.isNotEmpty())
        assertTrue("URL should contain https, got: '$url'", url.contains("https://"))
        assertTrue("URL should contain bnctestbed, got: '$url'", url.contains("bnctestbed"))

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
