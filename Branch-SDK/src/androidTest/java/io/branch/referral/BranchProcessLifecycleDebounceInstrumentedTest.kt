package io.branch.referral

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.referral.test.mock.MockActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Instrumented test for the 700 ms ON_STOP debounce built into Jetpack's
 * ProcessLifecycleOwner, which BranchProcessLifecycleObserver relies on.
 *
 * Because ProcessLifecycleOwner waits ~700 ms before dispatching ON_STOP
 * (to absorb Activity-to-Activity transitions and configuration changes),
 * a background period shorter than that window does NOT produce an ON_START
 * on return; a period longer than the window does.
 *
 * This test does NOT extend BranchTest to avoid the pre-existing compile
 * failure in BranchTest.java (setInitState / SESSION_STATE mismatch,
 * tracked by EMT-3875 / PR #1354).
 *
 * STACK ORDER TO LAND:
 *   1. #1366  -- adds BranchProcessLifecycleObserver (prototype, already on this branch)
 *   2. #1354  -- EMT-3875: fix androidTest compile (setInitState / canClearInitData breakage)
 *   3. this   -- adds targetSdk=34 + this debounce instrumented test
 *
 * KNOWN BLOCKER: the androidTest source set does not compile on the 6.0 line
 * until EMT-3875 (#1354) merges. Do NOT attempt to run this test before that.
 */
@RunWith(AndroidJUnit4::class)
class BranchProcessLifecycleDebounceInstrumentedTest {

    private val onStartCount = AtomicInteger(0)
    private var scenario: ActivityScenario<MockActivity>? = null
    private var testObserver: DefaultLifecycleObserver? = null

    @Before
    fun setUp() {
        onStartCount.set(0)
        // Observer must be added on the main thread (ProcessLifecycleOwner requirement).
        // Jetpack back-dispatches ON_START immediately if the process is already foregrounded.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val obs = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    onStartCount.incrementAndGet()
                }
            }
            testObserver = obs
            ProcessLifecycleOwner.get().lifecycle.addObserver(obs)
        }
        scenario = ActivityScenario.launch(MockActivity::class.java)
        // Allow the initial ON_START (and any back-dispatched lifecycle event) to settle.
        Thread.sleep(500L)
    }

    @After
    fun tearDown() {
        testObserver?.let { obs ->
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(obs)
            }
        }
        testObserver = null
        scenario?.close()
        scenario = null
    }

    /**
     * 300 ms in background is within Jetpack's ~700 ms ON_STOP debounce window.
     * ProcessLifecycleOwner must NOT dispatch a second ON_START when the app
     * returns to the foreground — the process lifecycle never fully transitioned
     * to the stopped state.
     */
    @Test
    fun onStart_shortBackground_withinDebounce_noSecondDispatch() {
        val before = onStartCount.get()

        pressHome()
        Thread.sleep(300L) // inside ~700 ms debounce window

        bringToForeground()
        Thread.sleep(500L) // settle

        assertEquals(
            "300 ms background (within debounce) must not trigger a second ON_START",
            before,
            onStartCount.get()
        )
    }

    /**
     * 1500 ms in background exceeds the ~700 ms ON_STOP debounce window.
     * ProcessLifecycleOwner must dispatch a second ON_START when the app
     * returns to the foreground — the process lifecycle fully transitioned
     * to stopped before the app re-entered the foreground.
     */
    @Test
    fun onStart_longBackground_outsideDebounce_secondDispatchFires() {
        val before = onStartCount.get()

        pressHome()
        Thread.sleep(1500L) // outside ~700 ms debounce window

        bringToForeground()
        Thread.sleep(500L) // settle

        assertEquals(
            "1500 ms background (beyond debounce) must trigger a second ON_START",
            before + 1,
            onStartCount.get()
        )
    }

    // ---------------------------------------------------------------------- helpers

    /** Sends the process to background via UiAutomation HOME global action. */
    private fun pressHome() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    /**
     * Brings the test process back to the foreground by re-launching MockActivity.
     * FLAG_ACTIVITY_REORDER_TO_FRONT returns the existing task to front without
     * creating a new Activity instance.
     */
    private fun bringToForeground() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(targetContext, MockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        targetContext.startActivity(intent)
    }
}
