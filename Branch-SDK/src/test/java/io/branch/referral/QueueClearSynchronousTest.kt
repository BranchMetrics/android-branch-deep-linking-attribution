package io.branch.referral

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

// Main work waits for runCurrent(), and USER_SET_WAIT_LOCK keeps the request at the head.
@OptIn(ExperimentalCoroutinesApi::class)
class QueueClearSynchronousTest : BranchTestBase() {

    private val app get() = RuntimeEnvironment.getApplication()
    private val main = StandardTestDispatcher()

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        Dispatchers.setMain(main)
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState =
            Lifecycle.State.CREATED
    }

    @Test
    fun requestEnqueuedAfterInitialize_survivesKeyChangeClear() {
        assertEquals(PrefHelper.NO_STRING_VALUE, PrefHelper.getInstance(app).getBranchKey())
        Branch.initialize(app, BranchConfiguration.Builder("key_live_test123").build())
        val request = lockedOpen()

        Branch.getInstance().requestQueue_.handleNewRequest(request)
        main.scheduler.runCurrent()

        val queue = BranchRequestQueue.getInstance(app)
        assertEquals(1, queue.getSize())
        assertSame(request, queue.peek())
    }

    @Test
    fun requestQueuedBeforeKeyChange_isRemovedWhenSetBranchKeyReturns() {
        Branch.initialize(app, BranchConfiguration.Builder("key_live_test123").build())
        Branch.getInstance().requestQueue_.handleNewRequest(lockedOpen())

        PrefHelper.getInstance(app).setBranchKey("key_live_other")

        assertEquals(0, BranchRequestQueue.getInstance(app).getSize())
    }

    private fun lockedOpen() = RequestOpen(app, null, false, null).apply {
        addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_SET_WAIT_LOCK)
    }
}
