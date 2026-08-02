package io.branch.referral

import android.os.SystemClock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowSystemClock
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.Duration

/**
 * EMT-3870: the request retry/timeout tracking must measure elapsed time on a monotonic clock
 * (SystemClock.elapsedRealtime), not System.currentTimeMillis. firstAttemptTime is captured on the
 * monotonic clock, so the timeout comparison must use the same clock; comparing a monotonic
 * firstAttemptTime against the wall clock (epoch) wrongly reports an immediate timeout.
 */
class RetryTimeoutMonotonicClockTest : BranchTestBase() {

    private val timeoutMs = 30_000L // mirrors BranchRequestQueue.REQUEST_TIMEOUT_MS
    private lateinit var retryInfoClass: Class<*>
    private lateinit var retryInfoCtor: Constructor<*>
    private lateinit var hasExceededTimeout: Method

    @Before
    fun setUp() {
        super.setUpBase()
        retryInfoClass = Class.forName("io.branch.referral.RequestRetryInfo")
        retryInfoCtor = retryInfoClass.getDeclaredConstructor(
            String::class.java,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType
        ).apply { isAccessible = true }
        hasExceededTimeout = retryInfoClass
            .getDeclaredMethod("hasExceededTimeout", Long::class.javaPrimitiveType)
            .apply { isAccessible = true }
    }

    private fun infoStartedAt(firstAttempt: Long): Any =
        retryInfoCtor.newInstance("r", firstAttempt, 0, firstAttempt, 0L)

    private fun timedOut(info: Any): Boolean =
        hasExceededTimeout.invoke(info, timeoutMs) as Boolean

    /**
     * firstAttemptTime is on the monotonic clock and no real time has elapsed, so the request is not
     * timed out. Before the fix, hasExceededTimeout compared this against the wall clock (epoch),
     * which is far larger than an uptime value, so it wrongly reported a timeout. RED before, GREEN after.
     */
    @Test
    fun timeoutIsMeasuredOnTheMonotonicClock() {
        val info = infoStartedAt(SystemClock.elapsedRealtime())
        assertFalse("a fresh request must not be timed out when tracked on the monotonic clock", timedOut(info))
    }

    /** Sanity: once the real timeout has elapsed on the monotonic clock, the request is timed out. */
    @Test
    fun realElapsedTimePastTimeout_doesTimeOut() {
        val info = infoStartedAt(SystemClock.elapsedRealtime())
        ShadowSystemClock.advanceBy(Duration.ofMillis(timeoutMs + 1_000))
        assertTrue("after the real timeout elapses, the request is timed out", timedOut(info))
    }
}
