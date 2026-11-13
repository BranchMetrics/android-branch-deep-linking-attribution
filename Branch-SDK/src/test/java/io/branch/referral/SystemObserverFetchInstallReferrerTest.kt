package io.branch.referral

import android.content.Context
import io.branch.data.InstallReferrerResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * Without the callback, INSTALL_REFERRER_FETCH_WAIT_LOCK is never removed and /v1/install is never sent.
 */
class SystemObserverFetchInstallReferrerTest {

    private val context = mock(Context::class.java)
    private val systemObserver = object : SystemObserver() {}
    private val callbackCount = AtomicInteger(0)
    private val callback = SystemObserver.InstallReferrerFetchEvents { callbackCount.incrementAndGet() }

    private fun fetchWith(fetch: (Context, Continuation<in InstallReferrerResult>) -> Any?) {
        systemObserver.fetchInstallReferrer(context, callback) { c, continuation -> fetch(c, continuation) }
    }

    @Test
    fun callbackRunsOnceWhenFetchReturnsNullWithoutSuspending() {
        fetchWith { _, _ -> null }

        assertEquals(1, callbackCount.get())
    }

    @Test
    fun callbackRunsOnceWhenFetchEndsWithErrorAfterSuspending() {
        var savedContinuation: Continuation<in InstallReferrerResult>? = null
        fetchWith { _, continuation ->
            savedContinuation = continuation
            COROUTINE_SUSPENDED
        }
        assertEquals(0, callbackCount.get())

        savedContinuation!!.resumeWith(Result.failure(RuntimeException("fetch failed")))

        assertEquals(1, callbackCount.get())
    }

    @Test
    fun callbackRunsOnceWhenFetchThrowsWithoutSuspending() {
        fetchWith { _, _ -> throw RuntimeException("fetch failed") }

        assertEquals(1, callbackCount.get())
    }

    @Test
    fun callbackRunsOnceWhenFetchResumesWithNullAfterSuspending() {
        var savedContinuation: Continuation<in InstallReferrerResult>? = null
        fetchWith { _, continuation ->
            savedContinuation = continuation
            COROUTINE_SUSPENDED
        }

        // fetchLatestInstallReferrer returns InstallReferrerResult?, so it resumes with null when nothing is found.
        @Suppress("UNCHECKED_CAST")
        (savedContinuation as Continuation<InstallReferrerResult?>).resumeWith(Result.success(null))

        assertEquals(1, callbackCount.get())
    }
}
