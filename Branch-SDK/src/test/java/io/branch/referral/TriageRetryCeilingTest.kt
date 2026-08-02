package io.branch.referral

import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Proof for EMT-3859: the modern coroutine [BranchRequestQueue] must not force-fail a request
 * that is merely waiting on a process-wait-lock once it crosses the retry counter (~500ms);
 * only the real 30s [BranchRequestQueue] timeout (or the stuck-lock resolver) may bound it.
 *
 * The original triage version of this test proved the BUG (the ~500ms ceiling fired before the
 * 30s timeout). It now asserts the CORRECTED behavior described in the ticket's acceptance
 * criteria.
 *
 * Exercises the production failure-decision logic directly via reflection, with no Android
 * framework or network involved, so the result is deterministic.
 */
class TriageRetryCeilingTest : BranchTestBase() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var retryInfoClass: Class<*>
    private lateinit var retryInfoCtor: Constructor<*>

    /** Production decision under test: should a not-yet-processable request be failed? */
    private lateinit var shouldFailRequest: Method

    @Before
    fun setUp() {
        super.setUpBase()

        // EMT-3870: RequestRetryInfo timestamps are monotonic (SystemClock.elapsedRealtime()).
        // Robolectric starts that clock at 100ms, so injecting an 11s-old lock would produce a
        // negative firstWaitLockTime and silently trip the `firstWaitLockTime > 0` guard rather
        // than the window under test. Advance past the longest age these tests inject, which is
        // what a real device's uptime always is.
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1))

        queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())

        // The real production RequestRetryInfo data class (file-private in BranchRequestQueue.kt).
        retryInfoClass = Class.forName("io.branch.referral.RequestRetryInfo")
        retryInfoCtor = retryInfoClass.getDeclaredConstructor(
            String::class.java,
            Long::class.javaPrimitiveType,   // firstAttemptTime
            Int::class.javaPrimitiveType,    // retryCount
            Long::class.javaPrimitiveType,   // lastAttemptTime
            Long::class.javaPrimitiveType    // firstWaitLockTime
        ).apply { isAccessible = true }

        // The corrected signature carries whether the request is purely waiting on a wait-lock.
        shouldFailRequest = BranchRequestQueue::class.java.getDeclaredMethod(
            "shouldFailRequest",
            retryInfoClass,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }
    }

    /** Build a production RequestRetryInfo with a given retry count and "first attempt" age. */
    private fun retryInfo(retryCount: Int, firstAttemptAgeMs: Long): Any {
        val now = SystemClock.elapsedRealtime()
        return retryInfoCtor.newInstance(
            "test-req",
            now - firstAttemptAgeMs, // firstAttemptTime
            retryCount,
            now,                     // lastAttemptTime
            0L                       // firstWaitLockTime
        )
    }

    private fun shouldFail(info: Any, waitingOnLock: Boolean): Boolean =
        shouldFailRequest.invoke(queue, info, waitingOnLock) as Boolean

    /**
     * AC: a request held on a wait-lock past the 5-retry ceiling is NOT failed with ERR_NO_SESSION.
     * retryCount=10 is double the old MAX_RETRY_ATTEMPTS, but the request is well under the 30s timeout.
     */
    @Test
    fun waitLocked_pastRetryLimit_isNotFailed() {
        val info = retryInfo(retryCount = 10, firstAttemptAgeMs = 0L)
        assertFalse(
            "A wait-locked request must not be force-failed by the retry counter (EMT-3859)",
            shouldFail(info, waitingOnLock = true)
        )
    }

    /**
     * AC: a wait-lock held ~2s does not fail prematurely; it only fails once the real 30s
     * REQUEST_TIMEOUT_MS is exceeded.
     */
    @Test
    fun waitLocked_failsOnlyAfterThirtySecondTimeout() {
        val heldTwoSeconds = retryInfo(retryCount = 50, firstAttemptAgeMs = 2_000L)
        assertFalse(
            "A wait-locked request held ~2s must not fail before the 30s timeout",
            shouldFail(heldTwoSeconds, waitingOnLock = true)
        )

        val pastTimeout = retryInfo(retryCount = 50, firstAttemptAgeMs = 31_000L)
        assertTrue(
            "A wait-locked request must fail once the 30s timeout is exceeded",
            shouldFail(pastTimeout, waitingOnLock = true)
        )
    }

    /**
     * No regression: a request that is NOT waiting on a wait-lock (e.g. genuinely missing session)
     * is still bounded by the retry limit, so the queue cannot spin forever.
     */
    @Test
    fun notWaitLocked_stillFailsAtRetryLimit() {
        val info = retryInfo(retryCount = 5, firstAttemptAgeMs = 0L)
        assertTrue(
            "A non-wait-locked request must still be bounded by the retry limit",
            shouldFail(info, waitingOnLock = false)
        )
    }

    /**
     * Safety net for the fix: the retry counter must keep incrementing while a request waits on a
     * lock. The stuck-lock heuristics (the `retryCount >= 3` USER_AGENT resolver) depend on it. If a
     * future change froze the counter for lock-waiters, the gate tests above would still pass but
     * the resolver would silently stop firing — this test pins that invariant.
     */
    @Test
    fun retryCounter_keepsIncrementing_forStuckLockHeuristics() {
        val increment = retryInfoClass.getDeclaredMethod("incrementRetry").apply { isAccessible = true }
        val getRetryCount = retryInfoClass.getDeclaredMethod("getRetryCount").apply { isAccessible = true }
        val hasExceededRetryLimit = retryInfoClass
            .getDeclaredMethod("hasExceededRetryLimit", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }

        val info = retryInfo(retryCount = 0, firstAttemptAgeMs = 0L)
        repeat(4) { increment.invoke(info) }

        assertEquals(4, getRetryCount.invoke(info))
        assertTrue(
            "retryCount must advance so the >=3 USER_AGENT stuck-lock heuristic stays reachable",
            hasExceededRetryLimit.invoke(info, 3) as Boolean
        )
    }

    /**
     * The 10s wait-lock resolver window must remain functional and is keyed off firstWaitLockTime,
     * independently of the retry counter — so it still bounds a lock-waiting request.
     */
    @Test
    fun waitLockTimeout_firesOffFirstWaitLockTime() {
        val now = SystemClock.elapsedRealtime()
        val info = retryInfoCtor.newInstance(
            "test-req",
            now,             // firstAttemptTime
            0,               // retryCount
            now,             // lastAttemptTime
            now - 11_000L    // firstWaitLockTime: lock first seen 11s ago
        )
        val hasExceededWaitLockTimeout = retryInfoClass
            .getDeclaredMethod("hasExceededWaitLockTimeout", Long::class.javaPrimitiveType)
            .apply { isAccessible = true }

        assertTrue(
            "A lock held >10s must trip the wait-lock resolver window",
            hasExceededWaitLockTimeout.invoke(info, 10_000L) as Boolean
        )
    }
}
