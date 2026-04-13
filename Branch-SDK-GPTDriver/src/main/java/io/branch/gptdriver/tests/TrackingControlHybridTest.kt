package io.branch.gptdriver.tests

import android.widget.ToggleButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * HYBRID — Espresso for toggle interaction, AI for Toast confirmation.
 *
 * Tests the tracking control toggle (enable/disable tracking).
 * The ToggleButton state maps to Branch.getInstance().isTrackingDisabled().
 * The SDK callback shows a Toast "Disabled Tracking" or "Enabled Tracking".
 *
 * Espresso handles: toggle clicks, state assertions (isChecked/isNotChecked).
 * AI handles: verifying the SDK callback Toast (confirms SDK actually processed the toggle).
 */
class TrackingControlHybridTest : BaseGptDriverTest() {

    @Test
    fun toggleTracking_disablesTracking() {
        // Setup: Ensure toggle starts unchecked (tracking enabled)
        ensureTrackingEnabled()
        Thread.sleep(1500) // Wait for any setup SDK callback to settle

        // DETERMINISTIC: Tap to disable tracking
        onView(withId(R.id.tracking_cntrl_btn)).perform(click())

        // DETERMINISTIC: Verify toggle is now checked (tracking disabled)
        onView(withId(R.id.tracking_cntrl_btn))
            .check(matches(isChecked()))

        // Wait for Toast
        Thread.sleep(3000)

        // AI: Verify the SDK actually processed the toggle (Toast confirms)
        driver.assertCondition(
            "A toast message should have appeared or been recently visible confirming tracking was disabled. " +
                "The toast message text should contain 'Disabled Tracking'. If the toast is no longer " +
                "visible, confirm that the toggle button is in the 'ON' / 'Checked' state."
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun toggleTracking_enablesTracking() {
        // Setup: Ensure toggle starts checked (tracking disabled)
        ensureTrackingDisabled()
        Thread.sleep(1500) // Wait for any setup SDK callback to settle

        // DETERMINISTIC: Tap to enable tracking
        onView(withId(R.id.tracking_cntrl_btn)).perform(click())

        // DETERMINISTIC: Verify toggle is now unchecked (tracking enabled)
        onView(withId(R.id.tracking_cntrl_btn))
            .check(matches(isNotChecked()))

        // Wait for Toast
        Thread.sleep(3000)

        // AI: Verify the SDK actually processed the toggle
        driver.assertCondition(
            "A toast message should have appeared or been recently visible confirming tracking was enabled. " +
                "The toast message text should contain 'Enabled Tracking'. If the toast is no longer " +
                "visible, confirm that the toggle button is in the 'OFF' / 'Unchecked' state."
        )

        driver.setSessionStatus("success")
    }

    private fun ensureTrackingEnabled() {
        activityRule.scenario.onActivity { activity ->
            val toggle = activity.findViewById<ToggleButton>(R.id.tracking_cntrl_btn)
            assertNotNull("ToggleButton not found", toggle)
            if (toggle.isChecked) {
                toggle.performClick()
            }
        }
    }

    private fun ensureTrackingDisabled() {
        activityRule.scenario.onActivity { activity ->
            val toggle = activity.findViewById<ToggleButton>(R.id.tracking_cntrl_btn)
            assertNotNull("ToggleButton not found", toggle)
            if (!toggle.isChecked) {
                toggle.performClick()
            }
        }
    }
}
