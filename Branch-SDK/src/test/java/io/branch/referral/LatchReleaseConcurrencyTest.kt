package io.branch.referral

import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * EMT-3899: Concurrency tests for the synchronous referring-params latch release.
 *
 * Covers two scenarios on a real Branch instance (not a mock) so the actual latch fields
 * and *Sync getter code paths are exercised end-to-end:
 *
 *  1. Release-on-init — when onInitSessionCompleted fires (simulated here by counting down the
 *     volatile latch field directly), the *Sync caller unblocks well before the 2500 ms timeout.
 *
 *  2. Timeout-fallback — when init never completes, getFirstReferringParamsSync() honours the
 *     LATCH_WAIT_UNTIL ceiling and returns a non-null JSONObject rather than blocking forever.
 *
 * Both tests depend on the EMT-3899 fixes:
 *  - The latch fields must be volatile so the write by the *Sync getter and the read by the
 *    releasing thread (onInitSessionCompleted / this test) are guaranteed to observe the same
 *    object without data races.
 *  - The releaser must capture a local reference before null-checking (TOCTOU fix) so that a
 *    concurrent timeout+null-assign on the *Sync side cannot interleave between the check and
 *    countDown().
 */
class LatchReleaseConcurrencyTest : BranchTestBase() {

    private lateinit var branch: Branch

    @Before
    fun setUp() {
        super.setUpBase()
        // Obtain a real Branch singleton via the standard factory — no mocking of Branch itself.
        // With Config.NONE the manifest is absent; getAutoInstance handles that gracefully (logs a
        // warning and sets NO_STRING_VALUE for the key).
        branch = Branch.getAutoInstance(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        // Reset the singleton so each test starts clean.
        Branch.shutDown()
    }

    /**
     * getLatestReferringParamsSync() blocks until the latch is counted down.
     * Releasing the latch (as onInitSessionCompleted does after the TOCTOU fix) must unblock
     * the caller well within the 2500 ms ceiling.
     */
    @Test
    fun getLatestReferringParamsSync_unblocks_when_latch_is_released() {
        val future = CompletableFuture<org.json.JSONObject>()
        Thread {
            // This call sets branch.getLatestReferringParamsLatch and then awaits it, because
            // the default initState is Uninitialized.
            future.complete(branch.getLatestReferringParamsSync())
        }.start()

        // Poll until the *Sync getter has installed the latch (it does so before awaiting, so
        // this converges in a few ms under any reasonable JVM scheduling).
        val deadline = System.currentTimeMillis() + 2000L
        while (branch.getLatestReferringParamsLatch == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }

        val latch = branch.getLatestReferringParamsLatch
        assertNotNull("latch should be non-null once getLatestReferringParamsSync is in-flight", latch)

        // Simulate what onInitSessionCompleted does after the EMT-3899 TOCTOU fix:
        // capture a local reference first, then count down.
        latch!!.countDown()

        // The *Sync getter must unblock and return long before the 2500 ms ceiling.
        val result = future.get(1000L, TimeUnit.MILLISECONDS)
        assertNotNull("getLatestReferringParamsSync must return a non-null JSONObject after latch release", result)
    }

    /**
     * getFirstReferringParamsSync() blocks when installParams is not yet available.
     * Without any init completing, it must time out on its own (LATCH_WAIT_UNTIL = 2500 ms) and
     * return a non-null JSONObject — it must never block indefinitely.
     *
     * Note: this test intentionally takes ~2.5 s because it exercises the real timeout path.
     */
    @Test
    fun getFirstReferringParamsSync_returns_after_timeout_without_init() {
        // PrefHelper returns NO_STRING_VALUE ("bnc_no_value") for installParams on a fresh context,
        // so getFirstReferringParamsSync will enter the await block and block for LATCH_WAIT_UNTIL.
        val future = CompletableFuture<org.json.JSONObject>()
        Thread {
            future.complete(branch.getFirstReferringParamsSync())
        }.start()

        // Allow LATCH_WAIT_UNTIL (2500 ms) plus a generous 1500 ms buffer for slow CI machines.
        val result = future.get(4000L, TimeUnit.MILLISECONDS)

        assertNotNull("getFirstReferringParamsSync must return a non-null JSONObject after timeout", result)
        assertTrue("result must be a JSONObject instance", result is org.json.JSONObject)
    }
}
