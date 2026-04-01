package io.branch.gptdriver.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * HYBRID — Espresso for button click and dialog detection, AI for option selection validation.
 *
 * Tests Consumer Protection Preference feature. The button opens an AlertDialog
 * with a list of 4 options: Full, Reduced, Minimal, None.
 * Selecting an option calls Branch.setConsumerProtectionAttributionLevel() and shows a Toast.
 */
class ConsumerProtectionHybridTest : BaseGptDriverTest() {

    @Test
    fun selectFullProtection_showsConfirmation() {
        // DETERMINISTIC: Scroll to and click "Consumer Protection Preference"
        onView(withId(R.id.cmdConsumerProtectionPreference))
            .perform(scrollTo(), click())

        // DETERMINISTIC: Verify dialog appeared with title
        onView(withText("Select Consumer Protection Attribution Level"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Select "Full" option
        onView(withText("Full")).perform(click())

        // AI: Verify the confirmation Toast
        driver.assertCondition(
            "A toast message appeared confirming the preference was set to 'Full'. " +
                "The toast should contain 'Consumer Protection Preference set to Full'."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun selectReducedProtection_showsConfirmation() {
        onView(withId(R.id.cmdConsumerProtectionPreference))
            .perform(scrollTo(), click())

        onView(withText("Select Consumer Protection Attribution Level"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Select "Reduced"
        onView(withText("Reduced")).perform(click())

        // AI: Verify Toast
        driver.assertCondition(
            "A toast message appeared confirming the preference was set to 'Reduced'."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun selectMinimalProtection_showsConfirmation() {
        onView(withId(R.id.cmdConsumerProtectionPreference))
            .perform(scrollTo(), click())

        onView(withText("Select Consumer Protection Attribution Level"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Select "Minimal"
        onView(withText("Minimal")).perform(click())

        // AI: Verify Toast
        driver.assertCondition(
            "A toast message appeared confirming the preference was set to 'Minimal'."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun selectNoneProtection_showsConfirmation() {
        onView(withId(R.id.cmdConsumerProtectionPreference))
            .perform(scrollTo(), click())

        onView(withText("Select Consumer Protection Attribution Level"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Select "None"
        onView(withText("None")).perform(click())

        // AI: Verify Toast
        driver.assertCondition(
            "A toast message appeared confirming the preference was set to 'None'."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun consumerProtectionDialog_showsAllOptions() {
        onView(withId(R.id.cmdConsumerProtectionPreference))
            .perform(scrollTo(), click())

        // AI: Validate all 4 options are visible in the dialog
        driver.assertBulk(
            listOf(
                "A dialog with title 'Select Consumer Protection Attribution Level' is visible",
                "The option 'Full' is visible in the list",
                "The option 'Reduced' is visible in the list",
                "The option 'Minimal' is visible in the list",
                "The option 'None' is visible in the list"
            )
        )

        // Dismiss by pressing back
        driver.execute("Press the back button or tap outside the dialog to dismiss it")

        driver.setSessionStatus("success")
    }
}
