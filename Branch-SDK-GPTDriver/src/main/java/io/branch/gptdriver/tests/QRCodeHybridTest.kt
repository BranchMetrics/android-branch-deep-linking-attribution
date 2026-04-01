package io.branch.gptdriver.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Test

/**
 * HYBRID — Espresso for actions, AI for visual validation.
 *
 * Tests QR Code generation. The "Create QR Code" button triggers an async
 * network call that returns a Bitmap displayed in an AlertDialog with an ImageView.
 *
 * Espresso handles: button click, scrolling.
 * AI handles: waiting for async QR generation, validating the image is visible,
 *             and that the dialog displays correctly.
 */
class QRCodeHybridTest : BaseGptDriverTest() {

    @Test
    fun createQRCode_displaysImageInDialog() {
        // DETERMINISTIC: Scroll to and click "Create QR Code"
        onView(withId(R.id.qrCode_btn))
            .perform(scrollTo(), click())

        // AI: Wait for async QR code generation and validate the dialog
        driver.assertBulk(
            listOf(
                "An alert dialog with title 'Your QR Code' is visible on screen",
                "The dialog contains a visible image (the QR code), not a blank or loading area",
                "There is a 'Dismiss' button at the bottom of the dialog"
            )
        )

        // AI: Dismiss the dialog
        driver.execute("Tap the 'Dismiss' button to close the QR code dialog")

        // AI: Verify we returned to the main screen
        driver.assertCondition(
            "The main screen of the Branch TestBed app is visible with buttons like " +
                "'Create Branch Link' and 'Create QR Code'"
        )

        driver.setSessionStatus("success")
    }

    @Test
    fun createQRCode_imageIsNotBlank() {
        // DETERMINISTIC: Scroll to and click "Create QR Code"
        onView(withId(R.id.qrCode_btn))
            .perform(scrollTo(), click())

        // AI: Wait and validate QR code has actual content (not blank/error)
        driver.assertCondition(
            "A dialog is visible with a QR code image. " +
                "The image contains a visible pattern (dark modules on light background), " +
                "indicating a real QR code was generated — not a blank, solid, or error image."
        )

        // AI: Dismiss dialog
        driver.execute("Tap 'Dismiss' to close the dialog")

        driver.setSessionStatus("success")
    }
}
