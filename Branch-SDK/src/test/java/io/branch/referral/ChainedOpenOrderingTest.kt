package io.branch.referral

import android.net.Uri
import io.branch.referral.network.BranchRemoteInterface
import io.branch.referral.util.BranchEvent
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import java.net.HttpURLConnection
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Orderings around the chained open that ChainedOpenDuringResolveTest does not reach, each with
 * the resolve held executing while the other requests are queued, and preferences kept.
 */
class ChainedOpenOrderingTest : BranchTestBase() {

    private class Resolve(val link: String, val status: Int) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
    }

    private val app get() = RuntimeEnvironment.getApplication()
    private val posts = Collections.synchronizedList(mutableListOf<String>())
    private val opens = Collections.synchronizedList(mutableListOf<JSONObject>())
    private val logs = Collections.synchronizedList(mutableListOf<String>())
    private val scripted = ConcurrentLinkedQueue<Resolve>()
    private val allResolves = Collections.synchronizedList(mutableListOf<Resolve>())

    @Before
    fun setUp() {
        super.setUpBase()
        Branch.shutDown()

        val remote = mock(BranchRemoteInterface::class.java)
        `when`(remote.make_restful_post(any(), any(), any(), any())).thenAnswer { invocation ->
            val post = JSONObject(invocation.getArgument<JSONObject>(0).toString())
            val url = invocation.getArgument<String>(1)
            val path = Defines.RequestPath.values().firstOrNull { url.endsWith(it.path) }?.path ?: url
            posts.add(path)
            if (path == Defines.RequestPath.EventsOpen.path) opens.add(post)
            val resolve = if (path == Defines.RequestPath.Deeplink.path) scripted.poll() else null
            when {
                resolve == null -> response(HttpURLConnection.HTTP_OK, JSONObject())
                else -> {
                    resolve.entered.countDown()
                    resolve.release.await(TIMEOUT_S, TimeUnit.SECONDS)
                    val body = JSONObject().put(Defines.Jsonkey.Data.key, linkData(resolve.link).toString())
                    response(resolve.status, body)
                }
            }
        }

        Branch.initialize(
            app,
            BranchConfiguration.Builder("key_live_test123")
                .setAutomaticOpenEvents(false)
                .setLogLevel(BranchLogger.BranchLogLevel.VERBOSE)
                .setLoggingCallback { message, _ -> if (message.startsWith("sendOpen")) logs.add(message) }
                .setRemoteInterface(remote)
                .build(),
        )
        Branch.getInstance().prefHelper_.apply {
            randomizedDeviceToken = "rdt"
            randomizedBundleToken = "rbt"
            sessionID = "sid"
        }
        awaitQueueProcessing()
    }

    @After
    fun tearDown() {
        allResolves.forEach { it.release.countDown() }
        BranchLogger.loggerCallback = null
        Branch.shutDown()
    }

    /** Without a resolved link, the foreground open is the only open, so it must survive. */
    @Test
    fun failedResolve_stillPostsTheForegroundOpen() {
        val resolve = holdResolve(LINK_ONE, BranchError.ERR_BRANCH_NO_CONNECTIVITY)
        startResolve(LINK_ONE, resolve)

        Branch.getInstance().sendOpen()
        resolve.release.countDown()

        assertOpensPosted(1)
        assertFalse(opens[0].has(LINK_DATA_KEY))
    }

    @Test
    fun callbackCopiedByRegisterAppInit_isNotReplacedAndFires() {
        val branch = Branch.getInstance()
        val resolve = holdResolve(LINK_ONE)
        startResolve(LINK_ONE, resolve)
        val fired = CountDownLatch(1)
        var error: BranchError? = null

        branch.sendOpen()
        branch.registerAppInit(branch.getInstallOrOpenRequest({ _, e -> error = e; fired.countDown() }, false), false)
        assertNotNull("registerAppInit did not copy its callback", (peekQueue() as RequestOpen).callback_)
        resolve.release.countDown()

        assertTrue("the init callback never fired", fired.await(TIMEOUT_S, TimeUnit.SECONDS))
        assertNull(error)
        val replaced = synchronized(logs) { logs.any { it.contains(REPLACED) } }
        assertFalse("replaced a callback-bearing open. logs: $logs", replaced)
        assertFalse(opens[0].has(LINK_DATA_KEY))
    }

    @Test
    fun secondResolveQueuedBeforeForegroundOpen_postsOneOpenWithTheFirstLink() {
        val branch = Branch.getInstance()
        val first = holdResolve(LINK_ONE)
        startResolve(LINK_ONE, first)
        holdResolve(LINK_TWO).release.countDown()

        branch.requestDeepLinkData(Uri.parse(LINK_TWO), null)
        branch.sendOpen()
        first.release.countDown()

        assertOpensPosted(1)
        assertEquals(2, postCount(Defines.RequestPath.Deeplink.path))
        assertEquals(LINK_ONE, opens[0].optJSONObject(LINK_DATA_KEY)?.optString(REFERRING_LINK))
    }

    @Test
    fun eventQueuedBehindForegroundOpen_postsAfterTheLinkCarryingOpen() {
        val resolve = holdResolve(LINK_ONE)
        startResolve(LINK_ONE, resolve)

        Branch.getInstance().sendOpen()
        BranchEvent("queued_behind_open").logEvent(app)
        resolve.release.countDown()

        assertTrue("the event was never posted", awaitPosts(Defines.RequestPath.TrackCustomEvent.path, 1))
        val order = synchronized(posts) { posts.toList() }
        val expected = listOf(
            Defines.RequestPath.Deeplink, Defines.RequestPath.EventsOpen, Defines.RequestPath.TrackCustomEvent,
        ).map { it.path }
        assertEquals(expected, order)
        assertEquals(LINK_ONE, opens[0].optJSONObject(LINK_DATA_KEY)?.optString(REFERRING_LINK))
    }

    private fun holdResolve(link: String, status: Int = HttpURLConnection.HTTP_OK) =
        Resolve(link, status).also { scripted.add(it); allResolves.add(it) }

    private fun startResolve(link: String, resolve: Resolve) {
        Branch.getInstance().requestDeepLinkData(Uri.parse(link), null)
        assertTrue("the resolve never reached the network", resolve.entered.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    private fun peekQueue() = BranchRequestQueue.getInstance(app).peek()

    private fun assertOpensPosted(count: Int) {
        assertTrue("fewer than $count opens posted: $posts", awaitPosts(Defines.RequestPath.EventsOpen.path, count))
        Thread.sleep(HOLD_MS)
        assertEquals("opens posted: $posts, logs: $logs", count, opens.size)
    }

    private fun awaitPosts(path: String, count: Int): Boolean {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_S)
        while (postCount(path) < count) {
            if (System.currentTimeMillis() > deadline) return false
            Thread.sleep(10)
        }
        return true
    }

    private fun postCount(path: String) = synchronized(posts) { posts.count { it == path } }

    private fun awaitQueueProcessing() {
        val queue = BranchRequestQueue.getInstance(app)
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_S)
        while (queue.queueState.value != BranchRequestQueue.QueueState.PROCESSING) {
            check(System.currentTimeMillis() < deadline) { "queue never started processing" }
            Thread.sleep(10)
        }
    }

    private fun linkData(link: String) = JSONObject().put("+clicked_branch_link", true).put(REFERRING_LINK, link)

    private fun response(status: Int, body: JSONObject) =
        ServerResponse("", status, "req-1", "").apply { setPost(body) }

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    private companion object {
        const val TIMEOUT_S = 5L
        const val HOLD_MS = 500L
        const val LINK_ONE = "https://bnctestbed.test-app.link/one"
        const val LINK_TWO = "https://bnctestbed.test-app.link/two"
        const val LINK_DATA_KEY = "link_data"
        const val REFERRING_LINK = "~referring_link"
        const val REPLACED = "sendOpen replaced"
    }
}
