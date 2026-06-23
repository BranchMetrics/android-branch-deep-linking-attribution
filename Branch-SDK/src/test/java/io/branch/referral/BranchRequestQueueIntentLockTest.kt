package io.branch.referral

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Method

/**
 * EMT-3860 backstop: once the INTENT_PENDING_WAIT_LOCK becomes live again, a request stuck on it
 * must be force-resolved by tryResolveStuckLocks after the 10s window — otherwise (with the
 * EMT-3859 fix removing the ~500ms ceiling) a cold start where onActivityResumed never fires would
 * hold the init request for the full 30s. Exercises the production tryResolveStuckLocks directly.
 */
class BranchRequestQueueIntentLockTest : BranchTestBase() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var tryResolveStuckLocks: Method

    @Before
    fun setUp() {
        super.setUpBase()
        queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())
        tryResolveStuckLocks = BranchRequestQueue::class.java.getDeclaredMethod(
            "tryResolveStuckLocks",
            ServerRequest::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    @Test
    fun stuckIntentPendingLock_isForceRemoved() {
        val request = mock(ServerRequest::class.java)
        tryResolveStuckLocks.invoke(queue, request, "INTENT_PENDING_WAIT_LOCK")
        verify(request).removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK)
    }

    @Test
    fun unrelatedLock_doesNotRemoveIntentPending() {
        val request = mock(ServerRequest::class.java)
        tryResolveStuckLocks.invoke(queue, request, "GAID_FETCH_WAIT_LOCK")
        verify(request, never())
            .removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK)
    }
}
