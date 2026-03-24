package io.branch.gptdriver.tests

import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * E2E test for Branch deep link creation using GPTDriver.
 *
 * All interactions are AI-driven via driver.execute() — GPTDriver
 * reads the screen and performs the actions autonomously.
 *
 * Docs: https://docs.mobileboost.io/gpt-driver-sdk/espresso/view-xml-based-apps/reference
 */
class LinkCreationTest : BaseGptDriverTest() {

    @Test
    fun createBranchLink_generatesValidUrl() {
        driver.execute("You should see the main screen with buttons. Tap on 'Create Branch Link'")

        driver.execute("Wait for the link to generate. The text field at the top should update with a URL")

        driver.assertCondition(
            "The text field at the top contains a URL that starts with 'https://' " +
                "and contains 'bnctestbed' in the domain"
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun createBranchLink_urlStartsWithHttps() {
        driver.execute("Tap on the 'Create Branch Link' button")

        driver.execute("Wait a few seconds for the link to appear in the text field at the top")

        driver.assertCondition("The generated URL in the text field starts with 'https://'")

        driver.setSessionStatus("success")
    }
}
