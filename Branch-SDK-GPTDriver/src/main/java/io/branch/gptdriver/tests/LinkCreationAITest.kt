package io.branch.gptdriver.tests

import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * AI approach — 100% GPTDriver.
 *
 * All actions and assertions are AI-driven via natural language.
 * The AI agent reads the screen and performs actions autonomously.
 *
 * Best for: dynamic UIs, visual validation, flows where resource IDs
 * are unavailable or the layout changes frequently.
 */
class LinkCreationAITest : BaseGptDriverTest() {

    @Test
    fun createBranchLink_generatesValidUrl() {
        // AI navigates: ensure we are on the main screen
        driver.execute(
            "You should see the main screen of the Branch TestBed app with buttons. " +
                "If not, press Back until you reach the main screen."
        )

        // AI taps the button
        driver.execute("Tap on the button that says 'Create Branch Link'")

        // AI waits for the result
        driver.execute(
            "Wait up to 5 seconds for a URL to appear in the text field " +
                "at the top of the screen. The URL should start with 'https://'."
        )

        // AI validates multiple conditions at once
        driver.assertBulk(
            listOf(
                "The text field at the top contains a URL that starts with 'https://'",
                "The URL in the text field contains 'bnctestbed' in the domain"
            )
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun createBranchLink_urlStartsWithHttps() {
        // AI taps the button
        driver.execute("Tap on the 'Create Branch Link' button")

        // AI waits for the result
        driver.execute("Wait a few seconds for the link to appear in the text field at the top")

        // AI validates t   he result
        driver.assertCondition("The generated URL in the text field starts with 'https://'")

        driver.setSessionStatus("success")
    }
}
