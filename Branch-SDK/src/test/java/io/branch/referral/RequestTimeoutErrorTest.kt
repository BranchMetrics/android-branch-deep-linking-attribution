package io.branch.referral

import android.os.SystemClock
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * EMT-3869: when the modern [BranchRequestQueue] fails a request because it exceeded the
 * process-lock-wait timeout, it must report a timeout-specific error
 * ([BranchError.ERR_BRANCH_REQ_TIMED_OUT]), not [BranchError.ERR_NO_SESSION], so callers can tell a
 * real timeout apart from a genuinely missing session. A retry-limit failure still uses
 * ERR_NO_SESSION.
 *
 * Drives the production handleRequestFailureWithCleanup directly via reflection — no Android
 * framework or network — so the result is deterministic.
 */
class RequestTimeoutErrorTest : BranchTestBase() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var retryInfoClass: Class<*>
    private lateinit var retryInfoCtor: Constructor<*>
    private lateinit var handleFailure: Method

    @Before
    fun setUp() {
        super.setUpBase()
        queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())

        retryInfoClass = Class.forName("io.branch.referral.RequestRetryInfo")
        retryInfoCtor = retryInfoClass.getDeclaredConstructor(
            String::class.java,
            Long::class.javaPrimitiveType,   // firstAttemptTime
            Int::class.javaPrimitiveType,    // retryCount
            Long::class.javaPrimitiveType,   // lastAttemptTime
            Long::class.javaPrimitiveType    // firstWaitLockTime
        ).apply { isAccessible = true }

        handleFailure = BranchRequestQueue::class.java.getDeclaredMethod(
            "handleRequestFailureWithCleanup",
            ServerRequest::class.java,
            String::class.java,
            retryInfoClass,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }
    }

    private fun retryInfo(retryCount: Int, firstAttemptAgeMs: Long): Any {
        val now = SystemClock.elapsedRealtime()
        return retryInfoCtor.newInstance("test-req", now - firstAttemptAgeMs, retryCount, now, 0L)
    }

    @Test
    fun timeoutFailure_reportsTimeoutSpecificError() {
        val request = mock(ServerRequest::class.java)
        // Exceeded the 30s timeout, and waiting on a lock so it reaches the timeout arm.
        handleFailure.invoke(queue, request, "test-req", retryInfo(retryCount = 1, firstAttemptAgeMs = 31_000), true)
        verify(request).handleFailure(eq(BranchError.ERR_BRANCH_REQ_TIMED_OUT), anyString())
    }

    @Test
    fun retryLimitFailure_keepsNoSessionError() {
        val request = mock(ServerRequest::class.java)
        // Exceeded the retry limit, not a timeout, not waiting on a lock.
        handleFailure.invoke(queue, request, "test-req", retryInfo(retryCount = 99, firstAttemptAgeMs = 0), false)
        verify(request).handleFailure(eq(BranchError.ERR_NO_SESSION), anyString())
    }
}
