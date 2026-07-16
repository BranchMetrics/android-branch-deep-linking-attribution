package io.branch.referral

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
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

/**
 * End-to-end proof for EMT-3859: drives the REAL production retry path
 * [BranchRequestQueue.handleRequestCannotBeProcessed] in a loop (not the isolated decision
 * function) and asserts the actual queue behavior changed.
 *
 * Before the fix, a request merely waiting on a process-wait-lock was force-failed with
 * ERR_NO_SESSION after MAX_RETRY_ATTEMPTS (5) * RETRY_DELAY_MS (100ms) ~= 500ms. After the fix it
 * survives indefinitely (bounded only by the 30s timeout). The complementary case proves the
 * retry ceiling still bounds a genuinely sessionless request, so the queue cannot spin forever.
 *
 * The suspend method is invoked via reflection with a hand-rolled Continuation, so no production
 * code is modified for testability and no kotlin-reflect dependency is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BranchRequestQueueWaitLockIntegrationTest : BranchTestBase() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var handleRequestCannotBeProcessed: Method

    @Before
    fun setUp() {
        super.setUpBase()
        queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())
        handleRequestCannotBeProcessed = BranchRequestQueue::class.java.getDeclaredMethod(
            "handleRequestCannotBeProcessed",
            ServerRequest::class.java,
            String::class.java,
            Continuation::class.java
        ).apply { isAccessible = true }
    }

    /** Invoke the real suspend retry cycle once and block until it resumes. */
    private fun driveRetryCycle(request: ServerRequest, requestId: String) {
        val latch = CountDownLatch(1)
        val continuation = object : Continuation<Any?> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Any?>) {
                latch.countDown()
            }
        }
        val result = handleRequestCannotBeProcessed.invoke(queue, request, requestId, continuation)
        if (result !== COROUTINE_SUSPENDED) {
            // Completed synchronously (e.g. the fail-and-return path, which does not delay).
            latch.countDown()
        }
        assertTrue("Retry cycle did not complete in time", latch.await(5, TimeUnit.SECONDS))
    }

    /**
     * A request held on a wait-lock through 8 retry cycles (~800ms, well past the old ~500ms
     * ceiling) must never be force-failed with ERR_NO_SESSION.
     */
    @Test
    fun waitLockedRequest_isNotFailed_pastOldFiveHundredMillisCeiling() {
        val request = mock(ServerRequest::class.java)
        `when`(request.isWaitingOnProcessToFinish).thenReturn(true)
        `when`(request.printWaitLocks()).thenReturn("GAID_FETCH_WAIT_LOCK")

        repeat(8) { driveRetryCycle(request, "wait-locked-req") }

        verify(request, never()).handleFailure(eq(BranchError.ERR_NO_SESSION), any())
    }

    /**
     * Non-regression: a request that is NOT waiting on a lock (genuinely no session) is still
     * bounded by the retry ceiling and fails, so a permanently un-processable request cannot spin
     * forever.
     */
    @Test
    fun nonWaitLockedRequest_stillFailsAtRetryCeiling() {
        val request = mock(ServerRequest::class.java)
        `when`(request.isWaitingOnProcessToFinish).thenReturn(false)
        `when`(request.printWaitLocks()).thenReturn("")

        // 6 cycles exceeds MAX_RETRY_ATTEMPTS (5), so the request must be failed.
        repeat(6) { driveRetryCycle(request, "no-session-req") }

        verify(request, atLeastOnce()).handleFailure(eq(BranchError.ERR_NO_SESSION), any())
    }
}
