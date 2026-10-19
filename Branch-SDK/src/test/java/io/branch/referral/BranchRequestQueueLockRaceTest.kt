package io.branch.referral

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

class BranchRequestQueueLockRaceTest : BranchTestBase() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var processNextRequest: Method

    @Before
    fun setUp() {
        super.setUpBase()
        // A queue whose processing loop never started, so only this test calls processNextRequest.
        queue = BranchRequestQueue::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }
            .newInstance(RuntimeEnvironment.getApplication())
        processNextRequest = BranchRequestQueue::class.java
            .getDeclaredMethod("processNextRequest", Continuation::class.java)
            .apply { isAccessible = true }
    }

    @Suppress("UNCHECKED_CAST")
    private fun queueList(): MutableList<ServerRequest> =
        BranchRequestQueue::class.java.getDeclaredField("queueList")
            .apply { isAccessible = true }
            .get(queue) as MutableList<ServerRequest>

    private fun process() {
        val latch = CountDownLatch(1)
        val continuation = object : Continuation<Any?> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Any?>) {
                latch.countDown()
            }
        }
        if (processNextRequest.invoke(queue, continuation) !== COROUTINE_SUSPENDED) {
            latch.countDown()
        }
        assertTrue("processNextRequest did not finish", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun lockRemovedAfterTheCheckSawIt_requestIsNotFailed() {
        var locked = true
        var removeLockOnNextPrint = false
        val request = mock(ServerRequestInitSession::class.java)
        `when`(request.isWaitingOnProcessToFinish).thenAnswer { locked }
        // canProcessRequest prints the locks after reading them: the GAID callback lands there.
        `when`(request.printWaitLocks()).thenAnswer {
            if (removeLockOnNextPrint) locked = false
            "[GAID_FETCH_WAIT_LOCK]"
        }
        queueList().add(request)

        // Past the retry limit while the advertising ID is still being read.
        repeat(6) { process() }
        removeLockOnNextPrint = true
        process()

        verify(request, never()).handleFailure(eq(BranchError.ERR_NO_SESSION), any())
    }
}
