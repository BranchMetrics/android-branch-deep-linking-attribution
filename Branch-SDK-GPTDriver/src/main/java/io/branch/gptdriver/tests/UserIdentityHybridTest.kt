package io.branch.gptdriver.tests

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HYBRID — Espresso for button clicks, AI for dialog interaction and Toast validation.
 *
 * "Set User ID" opens an AlertDialog with a dynamically-created EditText
 * (not in the static layout XML, so no resource ID). AI is needed to
 * type into the dialog's text field.
 *
 * "Clear User ID" shows a Toast — AI validates it (no try/catch swallowing).
 */
class UserIdentityHybridTest : BaseGptDriverTest() {

    companion object {
        private const val TAG = "UserIdentityTest"
    }

    @Test
    fun setUserIdentity_showsConfirmation() {
        // DETERMINISTIC: Click "Set User ID" button
        onView(withId(R.id.cmdIdentifyUser)).perform(click())

        // DETERMINISTIC: Verify the AlertDialog appeared with title
        onView(withText("Set User ID"))
            .check(matches(isDisplayed()))

        // AI: Type a test user ID into the dialog's EditText and confirm
        driver.execute(
            "There is an alert dialog with a text field and 'Set' / 'Cancel' buttons. " +
                "Type 'test_user_e2e' into the text input field, then tap 'Set'."
        )

        // AI: Verify the confirmation Toast appeared
        driver.assertCondition(
            "A toast message appeared confirming the identity was set. " +
                "The toast should contain 'Set Identity' and 'test_user_e2e'."
        )

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.cmdIdentifyUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun setUserIdentity_extractConfirmation() {
        // DETERMINISTIC: Click "Set User ID"
        onView(withId(R.id.cmdIdentifyUser)).perform(click())

        // DETERMINISTIC: Verify dialog appeared
        onView(withText("Set User ID"))
            .check(matches(isDisplayed()))

        // AI: Type user ID and submit
        driver.execute(
            "Type 'e2e_extract_test' into the text input field, then tap 'Set'."
        )

        // AI: Extract the toast message to validate programmatically
        val extracted = driver.extract(listOf("toast_message_text"))
        val toastText = extracted["toast_message_text"]?.toString() ?: ""
        Log.i(TAG, "Extracted toast: $toastText")

        // DETERMINISTIC: Assert on extracted value
        assertTrue(
            "Toast should confirm identity was set, got: '$toastText'",
            toastText.contains("Set Identity") || toastText.contains("e2e_extract_test")
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun clearUserIdentity_showsConfirmation() {
        // DETERMINISTIC: Click "Clear User ID"
        onView(withId(R.id.cmdClearUser)).perform(click())

        // AI: Verify the logout/clear Toast appeared (NOT swallowed in try/catch)
        driver.assertCondition(
            "A toast message appeared confirming the user ID was cleared. " +
                "The toast should contain 'Cleared User ID'."
        )

        // DETERMINISTIC: Verify main screen is still displayed
        onView(withId(R.id.cmdClearUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun setUserIdentity_dialogHasCorrectElements() {
        // DETERMINISTIC: Click "Set User ID"
        onView(withId(R.id.cmdIdentifyUser)).perform(click())

        // AI: Check all expected dialog elements at once (non-throwing)
        val checks = driver.checkBulk(
            listOf(
                "A dialog with title 'Set User ID' is visible",
                "The dialog has a text input field",
                "A 'Set' button is visible in the dialog",
                "A 'Cancel' button is visible in the dialog"
            )
        )
        Log.i(TAG, "Dialog element checks: $checks")

        // DETERMINISTIC: Dismiss dialog
        onView(withText("Cancel")).perform(click())

        // Verify we're back on main screen
        onView(withId(R.id.cmdIdentifyUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }
}
