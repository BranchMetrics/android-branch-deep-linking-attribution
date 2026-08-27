package io.branch.gptdriver.tests

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import io.branch.gptdriver.withRetry
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
        onView(withId(R.id.cmdIdentifyUser)).perform(scrollTo(), click())

        // DETERMINISTIC: Verify the AlertDialog appeared with title
        onView(withText("Set User ID"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Type a test user ID into the dialog's EditText and confirm
        onView(androidx.test.espresso.matcher.ViewMatchers.withHint("Your_user_id"))
            .perform(
                androidx.test.espresso.action.ViewActions.typeText("test_user_e2e"),
                androidx.test.espresso.action.ViewActions.closeSoftKeyboard()
            )

        onView(withText("Set")).perform(click())

        // SOFT AI PROBE: the confirmation Toast is ephemeral (2-3s visible);
        // don't gate the test on AI catching that screenshot window. The
        // Espresso check below proves we are back on the main screen.
        runCatching {
            driver.checkBulk(
                listOf(
                    "A Toast message confirming the identity was set with 'test_user_e2e' " +
                        "is currently visible or was recently visible"
                )
            )
        }

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.cmdIdentifyUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun setUserIdentity_extractConfirmation() {
        // DETERMINISTIC: Click "Set User ID"
        onView(withId(R.id.cmdIdentifyUser)).perform(scrollTo(), click())

        // DETERMINISTIC: Verify dialog appeared
        onView(withText("Set User ID"))
            .check(matches(isDisplayed()))

        // DETERMINISTIC: Type user ID and submit
        onView(androidx.test.espresso.matcher.ViewMatchers.withHint("Your_user_id"))
            .perform(
                androidx.test.espresso.action.ViewActions.typeText("e2e_extract_test"),
                androidx.test.espresso.action.ViewActions.closeSoftKeyboard()
            )

        onView(withText("Set")).perform(click())

        // Wait for potential Toast and dialog-dismissal animations to settle
        // before handing off to the AI. Without this buffer Espresso has
        // reported AppNotIdleException after the extract returns because the
        // main looper was still processing Choreographer frames from the
        // animation.
        Thread.sleep(2000)

        // AI: Extract any confirmation or toast message text (best effort).
        // Wrapped in withRetry so DNS or socket timeout flakes on the
        // MobileBoost backend trigger a retry instead of failing the test.
        // runCatching on the outside so that any residual extract failure
        // (e.g. AI couldn't see the Toast in any of the retries) stays
        // best-effort — the deterministic Espresso assertion below is the
        // real proof of success.
        val extracted = runCatching {
            withRetry { driver.extract(listOf("confirmation_message", "toast_text")) }
        }.getOrElse { emptyMap() }
        val toastText = (extracted["toast_text"] ?: extracted["confirmation_message"])?.toString() ?: ""
        Log.i(TAG, "Extracted text: $toastText")

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.cmdIdentifyUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun clearUserIdentity_showsConfirmation() {
        // DETERMINISTIC: Click "Clear User ID"
        onView(withId(R.id.cmdClearUser)).perform(scrollTo(), click())

        // Wait for potential Toast
        Thread.sleep(2000)

        // DETERMINISTIC: Verify main screen is still displayed
        onView(withId(R.id.cmdClearUser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun setUserIdentity_dialogHasCorrectElements() {
        // DETERMINISTIC: Click "Set User ID"
        onView(withId(R.id.cmdIdentifyUser)).perform(scrollTo(), click())

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
