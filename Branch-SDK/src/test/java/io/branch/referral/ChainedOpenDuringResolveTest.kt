package io.branch.referral

import android.net.Uri
import io.branch.interfaces.IBranchLoggingCallbacks
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import java.net.HttpURLConnection
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A cold link launch with prefs kept: the resolve is executing when the foreground open is queued,
 * and the open that reaches the network has to be the one carrying the resolved link_data.
 */
class ChainedOpenDuringResolveTest : BranchTestBase() {

    private val timeline = Collections.synchronizedList(mutableListOf<String>())
    private val openPosts = Collections.synchronizedList(mutableListOf<JSONObject>())
    private val resolveEntered = CountDownLatch(1)
    private val releaseResolve = CountDownLatch(1)
    private val oneOpenPosted = CountDownLatch(1)
    private val twoOpensPosted = CountDownLatch(2)

    @Before
    fun setUp() {
        super.setUpBase()
        Branch.shutDown()

        val remote = mock(BranchRemoteInterface::class.java)
        `when`(remote.make_restful_post(any(), any(), any(), any())).thenAnswer { invocation ->
            val post = JSONObject(invocation.getArgument<JSONObject>(0).toString())
            val url = invocation.getArgument<String>(1)
            when {
                url.endsWith(Defines.RequestPath.Deeplink.path) -> {
                    timeline.add(POST_RESOLVE)
                    resolveEntered.countDown()
                    releaseResolve.await(TIMEOUT_S, TimeUnit.SECONDS)
                    response(JSONObject().put(Defines.Jsonkey.Data.key, LINK_DATA.toString()))
                }
                url.endsWith(Defines.RequestPath.EventsOpen.path) -> {
                    timeline.add("POST v3/events/open link_data=${post.has(LINK_DATA_KEY)}")
                    openPosts.add(post)
                    oneOpenPosted.countDown()
                    twoOpensPosted.countDown()
                    response(JSONObject())
                }
                else -> {
                    timeline.add("POST $url")
                    response(JSONObject())
                }
            }
        }

        Branch.initialize(
            RuntimeEnvironment.getApplication(),
            BranchConfiguration.Builder("key_live_test123")
                .setAutomaticOpenEvents(false)
                .setLogLevel(BranchLogger.BranchLogLevel.VERBOSE)
                .setLoggingCallback(recordingLogger)
                .setRemoteInterface(remote)
                .build(),
        )
        awaitQueueProcessing()
    }

    @After
    fun tearDown() {
        releaseResolve.countDown()
        BranchLogger.loggerCallback = null
        Branch.shutDown()
    }

    @Test
    fun foregroundOpenQueuedDuringResolve_postsOneOpenWithLinkData() {
        runColdLinkLaunch { it.sendOpen() }
    }

    @Test
    fun consumerProtectionOpenQueuedDuringResolve_postsOneOpenWithLinkData() {
        runColdLinkLaunch { it.setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL) }
    }

    private fun runColdLinkLaunch(foregroundProducer: (Branch) -> Unit) {
        val branch = Branch.getInstance()
        val queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())

        branch.requestDeepLinkData(Uri.parse(LINK), null)
        assertTrue("the resolve never reached the network", resolveEntered.await(TIMEOUT_S, TimeUnit.SECONDS))

        foregroundProducer(branch)
        assertTrue("the foreground open is not queued behind the resolve", queue.peek() is RequestOpen)
        assertFalse("an open was posted while the resolve was held", oneOpenPosted.await(HOLD_MS, TimeUnit.MILLISECONDS))

        timeline.add(RELEASE)
        releaseResolve.countDown()
        assertTrue("no open was posted after the resolve returned", oneOpenPosted.await(TIMEOUT_S, TimeUnit.SECONDS))
        assertFalse("a second open was posted", twoOpensPosted.await(HOLD_MS, TimeUnit.MILLISECONDS))

        val events = synchronized(timeline) { timeline.toList() }
        val dump = "timeline: $events"
        // The held resolve only proves ordering while the queue runs a single processing loop.
        assertEquals("processing loops started. $dump", 1, events.count { it.contains(LOOP_STARTED) })
        assertEquals("resolves posted. $dump", 1, events.count { it == POST_RESOLVE })
        assertTrue(
            "the chained sendOpen(JSONObject) never ran. $dump",
            events.drop(events.indexOf(RELEASE) + 1).any { it.contains(SEND_OPEN_ENTRY) },
        )
        assertEquals("opens posted. $dump", 1, openPosts.size)
        assertTrue("the only open posted carries no link_data. $dump", openPosts[0].has(LINK_DATA_KEY))
        assertEquals(LINK_DATA.toString(), openPosts[0].getJSONObject(LINK_DATA_KEY).toString())
    }

    private val recordingLogger = IBranchLoggingCallbacks { message, _ ->
        if (message.contains(LOOP_STARTED) || message.startsWith("sendOpen")) {
            timeline.add("LOG $message")
        }
    }

    private fun awaitQueueProcessing() {
        val queue = BranchRequestQueue.getInstance(RuntimeEnvironment.getApplication())
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_S)
        while (queue.queueState.value != BranchRequestQueue.QueueState.PROCESSING) {
            check(System.currentTimeMillis() < deadline) { "queue never started processing" }
            Thread.sleep(10)
        }
    }

    private fun response(body: JSONObject) =
        ServerResponse("", HttpURLConnection.HTTP_OK, "req-1", "Success").apply { setPost(body) }

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    private companion object {
        const val TIMEOUT_S = 5L
        const val HOLD_MS = 500L
        const val LINK = "https://bnctestbed.test-app.link/XcVyairAa6b"
        const val LINK_DATA_KEY = "link_data"
        const val POST_RESOLVE = "POST v3/deeplink"
        const val RELEASE = "RELEASE resolve"
        const val LOOP_STARTED = "BranchRequestQueue.startProcessing called"
        const val SEND_OPEN_ENTRY = "sendOpen BranchAttributionLevel"
        val LINK_DATA: JSONObject = JSONObject()
            .put("+clicked_branch_link", true)
            .put("~referring_link", LINK)
    }
}
