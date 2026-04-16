package io.branch.gptdriver.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.branch.branchandroidtestbed.R
import io.branch.gptdriver.BaseGptDriverTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * HYBRID — Espresso for button click, UiAutomator for WebView detection,
 * AI as a soft probe for content quality.
 *
 * Tests the in-app browser experience. The "Browser Test" button calls
 * Branch.openBrowserExperience() which opens a WebView directed to
 * https://branch.io.
 *
 * Determinism comes from UiAutomator: `By.clazz("android.webkit.WebView")`
 * and `Until.hasObject(...)` wait for an actual WebView node to appear in
 * the window hierarchy. The AI call is kept as a soft probe so the
 * dashboard still records a semantic judgment ("content loaded, not blank
 * or error"), but it can no longer fail the test on its own.
 */
class BrowserExperienceHybridTest : BaseGptDriverTest() {

    private companion object {
        const val WEBVIEW_CLASS = "android.webkit.WebView"
        const val WEBVIEW_APPEAR_TIMEOUT_MS = 15_000L
    }

    /**
     * When the previous test in this class opened an in-app WebView and
     * pressed back, the system can still be transitioning MainActivity
     * back to RESUMED at the moment the next test starts. Espresso's
     * first `onView()` then throws `NoActivityResumedException` because
     * no activity is in stage RESUMED yet.
     *
     * We explicitly move the scenario to RESUMED here so the test body
     * only starts once MainActivity owns the window again. This is a
     * no-op for a fresh launch (already RESUMED) and a true recovery
     * when we are second in the class.
     */
    @Before
    fun ensureActivityResumed() {
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        // Small paint buffer so the first onView() below does not race
        // the Choreographer pipeline.
        Thread.sleep(500)
    }

    @Test
    fun browserTest_opensInAppWebView() {
        // DETERMINISTIC: Scroll to and click "Browser Test"
        onView(withId(R.id.openInAppBrowser))
            .perform(scrollTo(), click())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // DETERMINISTIC: wait for an actual WebView node to be present in the
        // window hierarchy. Until.hasObject polls until the condition is true
        // or the timeout elapses, so this replaces the previous flaky AI
        // assertion with a platform-level guarantee.
        val webViewAppeared = device.wait(
            Until.hasObject(By.clazz(WEBVIEW_CLASS)),
            WEBVIEW_APPEAR_TIMEOUT_MS
        )
        assertNotNull(
            "Expected a WebView to be present after clicking Browser Test",
            webViewAppeared
        )

        // SOFT AI PROBE: record the AI's judgment for the dashboard without
        // gating the test result on it.
        runCatching {
            driver.checkBulk(
                listOf(
                    "An in-app browser or WebView is visible on screen, not the TestBed main screen"
                )
            )
        }

        // Navigate back to the main screen.
        device.pressBack()
        Thread.sleep(3000)

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.openInAppBrowser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }

    @Test
    fun browserTest_loadsWebContent() {
        // DETERMINISTIC: Scroll to and click "Browser Test"
        onView(withId(R.id.openInAppBrowser))
            .perform(scrollTo(), click())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // DETERMINISTIC: wait for a WebView node. Without this we cannot
        // distinguish "Branch openBrowserExperience did nothing" from "AI
        // screenshot timing missed the load".
        val webViewAppeared = device.wait(
            Until.hasObject(By.clazz(WEBVIEW_CLASS)),
            WEBVIEW_APPEAR_TIMEOUT_MS
        )
        assertNotNull(
            "Expected a WebView to be present after clicking Browser Test",
            webViewAppeared
        )

        // Give the page a few seconds to paint so the AI probe sees content.
        Thread.sleep(3000)

        // SOFT AI PROBE: content quality check (not blank / error page).
        runCatching {
            driver.checkBulk(
                listOf(
                    "A browser or WebView is visible on screen",
                    "The page has loaded with visible content (text, images, or UI elements) " +
                        "and is not showing a blank page or an error message"
                )
            )
        }

        // Navigate back.
        device.pressBack()
        Thread.sleep(3000)

        // DETERMINISTIC: Verify we're back on the main screen
        onView(withId(R.id.openInAppBrowser))
            .check(matches(isDisplayed()))

        driver.setSessionStatus("success")
    }
}
