package io.branch.referral

import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Calling initialize() more than once must not start a second processing loop, which would send a
 * request while the one ahead of it is still in flight.
 */
class BranchRequestQueueSingleLoopTest : BranchTestBase() {

    private val inFlight = AtomicInteger(0)
    private val maxInFlight = AtomicInteger(0)
    private val posts = ConcurrentHashMap<String, AtomicInteger>()
    private val entered = mapOf(R1 to CountDownLatch(1), R2 to CountDownLatch(1))
    private val release = mapOf(R1 to CountDownLatch(1), R2 to CountDownLatch(1))
    private val finished = mapOf(R1 to CountDownLatch(1), R2 to CountDownLatch(1))

    @Before
    fun setUp() {
        Branch.shutDown()

        val remote = mock(BranchRemoteInterface::class.java)
        `when`(remote.make_restful_post(any(), any(), any(), any())).thenAnswer { invocation ->
            val tag = invocation.getArgument<JSONObject>(0).optString(TAG_KEY)
            posts.computeIfAbsent(tag) { AtomicInteger(0) }.incrementAndGet()
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet()) { a, b -> maxOf(a, b) }
            try {
                entered[tag]?.countDown()
                release[tag]?.await(TIMEOUT_S, TimeUnit.SECONDS)
            } finally {
                inFlight.decrementAndGet()
                finished[tag]?.countDown()
            }
            ServerResponse("", HttpURLConnection.HTTP_OK, "req-1", "Success").apply {
                setPost(JSONObject().put("url", URL))
            }
        }

        Branch.initialize(
            RuntimeEnvironment.getApplication(),
            BranchConfiguration.Builder("key_live_test123")
                .setAutomaticOpenEvents(false)
                .setRemoteInterface(remote)
                .build(),
        )
    }

    @After
    fun tearDown() {
        release.values.forEach { it.countDown() }
        Branch.shutDown()
    }

    @Test
    fun initializeCalledTwice_sendsOneRequestAtATime() {
        val queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())
        queue.initialize()
        queue.initialize()

        queue.enqueue(createUrl(R1))
        assertTrue("r1 never reached the network", entered.getValue(R1).await(TIMEOUT_S, TimeUnit.SECONDS))
        queue.enqueue(createUrl(R2))
        // An idle second loop takes r2's trigger in milliseconds; the bound is what a passing run waits.
        entered.getValue(R2).await(HOLD_MS, TimeUnit.MILLISECONDS)

        release.values.forEach { it.countDown() }
        assertTrue(
            "r1 and r2 did not both finish",
            finished.values.all { it.await(TIMEOUT_S, TimeUnit.SECONDS) },
        )
        assertEquals("requests in flight at once", 1, maxInFlight.get())
        assertEquals("r1 posts", 1, posts[R1]?.get())
        assertEquals("r2 posts", 1, posts[R2]?.get())
    }

    private fun createUrl(tag: String) = ServerRequestCreateUrl(
        Defines.RequestPath.GetURL,
        JSONObject().put(TAG_KEY, tag),
        RuntimeEnvironment.getApplication(),
    )

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    private companion object {
        const val TIMEOUT_S = 5L
        const val HOLD_MS = 2_000L
        const val TAG_KEY = "test_tag"
        const val R1 = "r1"
        const val R2 = "r2"
        const val URL = "https://test.app.link/abc"
    }
}
