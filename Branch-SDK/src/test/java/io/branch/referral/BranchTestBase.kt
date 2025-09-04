package io.branch.referral

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Base test class that sets up Android mocking and coroutines test environment.
 * All Branch SDK unit tests should extend this class to get proper Android framework mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
abstract class BranchTestBase {

    protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    open fun setUpBase() {
        // Initialize Mockito
        MockitoAnnotations.openMocks(this)
        
        // Set up coroutines test dispatcher
        Dispatchers.setMain(testDispatcher)
        
        // Enable BranchLogger for tests
        BranchLogger.loggingEnabled = true
        
        // Set up Looper for main thread operations
        val shadowMainLooper = Shadows.shadowOf(Looper.getMainLooper())
        shadowMainLooper.idle()
    }

    @After
    open fun tearDownBase() {
        // Reset coroutines main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Helper method to run all pending main looper tasks
     */
    protected fun runMainLooperTasks() {
        val shadowMainLooper = Shadows.shadowOf(Looper.getMainLooper())
        shadowMainLooper.idle()
    }

    /**
     * Helper method to advance time by the specified amount
     */
    protected fun advanceTimeBy(millis: Long) {
        val shadowMainLooper = Shadows.shadowOf(Looper.getMainLooper())
        shadowMainLooper.idleFor(java.time.Duration.ofMillis(millis))
    }

    /**
     * Create a mock Handler for testing
     */
    protected fun createMockHandler(): Handler {
        val mockHandler = Mockito.mock(Handler::class.java)
        // Configure mock handler to execute posts immediately for testing
        Mockito.`when`(mockHandler.post(Mockito.any())).thenAnswer { invocation ->
            val runnable = invocation.getArgument<Runnable>(0)
            runnable.run()
            true
        }
        Mockito.`when`(mockHandler.postDelayed(Mockito.any(), Mockito.anyLong())).thenAnswer { invocation ->
            val runnable = invocation.getArgument<Runnable>(0)
            runnable.run()
            true
        }
        return mockHandler
    }
}