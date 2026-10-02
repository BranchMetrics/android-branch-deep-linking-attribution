package io.branch.referral

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * A launch's wait locks are added and removed from different threads: the launch adds them, and the
 * install referrer and ad ID reads each remove theirs from their own thread when they finish.
 */
class ServerRequestWaitLockTest : BranchTestBase() {

    @Before
    fun setUpBranch() {
        Branch.initialize(RuntimeEnvironment.getApplication(), BranchConfiguration.Builder("key_live_test123").build())
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
    }

    @Test
    fun locksChangedFromTwoThreadsAtOnce_leaveNothingToWaitFor() {
        val request = RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
        val start = CountDownLatch(1)
        val workers = listOf(
            ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK,
            ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK,
        ).map { lock ->
            thread {
                start.await()
                repeat(ROUNDS) {
                    request.addProcessWaitLock(lock)
                    request.removeProcessWaitLock(lock)
                }
            }
        }

        start.countDown()
        workers.forEach { it.join(TimeUnit.SECONDS.toMillis(30)) }

        assertTrue("the threads never finished", workers.none { it.isAlive })
        assertEquals("[]", request.printWaitLocks())
        assertFalse("every lock was removed, so nothing is left to wait for", request.isWaitingOnProcessToFinish)
    }

    private companion object {
        const val ROUNDS = 200_000
    }
}
