package io.branch.gptdriver

import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import org.hamcrest.Matcher

/**
 * Reads the generated link out of the TestBed's short-URL field.
 *
 * Espresso asserts rather than extracts, so pulling a value back needs a
 * ViewAction that captures it. The L1 scenario drivers need the actual URL to
 * build an ACTION_VIEW intent. The drivers never use the vendor key; they read the
 * link here rather than through the AI driver. scripts/run_l1_instrumented.sh still
 * asks for that key, although the drivers do not need it.
 */
object LinkFieldReader {

    fun read(): String {
        var captured = ""
        onView(withId(R.id.editReferralShortUrl)).perform(capture { captured = it })
        return captured
    }

    private fun capture(onRead: (String) -> Unit) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(EditText::class.java)

        override fun getDescription(): String = "read the short-URL field"

        override fun perform(uiController: UiController, view: View) {
            onRead((view as EditText).text.toString())
        }
    }
}
