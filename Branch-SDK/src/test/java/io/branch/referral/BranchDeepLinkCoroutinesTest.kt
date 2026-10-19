package io.branch.referral

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.referral.network.BranchRemoteInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class BranchDeepLinkCoroutinesTest : BranchTestBase() {

    private class StubRemoteInterface(
        private val responseCode: Int,
        private val body: String
    ) : BranchRemoteInterface() {
        val openSent = CountDownLatch(1)

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse(body, responseCode)
        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            if (url.orEmpty().endsWith("v3/events/open")) openSent.countDown()
            return BranchResponse(body, responseCode)
        }
    }

    /** Blocks the single queue consumer so a following request stays queued and unsent. */
    private class GatedRemoteInterface : BranchRemoteInterface() {
        val entered = CountDownLatch(1)
        val gate = CountDownLatch(1)

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)
        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            entered.countDown()
            gate.await(10, TimeUnit.SECONDS)
            return BranchResponse("{}", 200)
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()
    private val uri: Uri get() = Uri.parse("https://example.app.link/abc123")

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())
        Branch._userAgentString = "test-agent"
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState =
            Lifecycle.State.CREATED
    }

    @Test
    fun returnsTheReferringParams() = runTest {
        val remote = StubRemoteInterface(200, """{"data":"{\"~channel\":\"email\",\"foo\":\"bar\"}"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)

        val params = Branch.getInstance().requestDeepLinkData(uri)

        assertEquals("bar", params.optString("foo"))
        assertEquals("email", params.optString("~channel"))
        awaitTheLaunchOpen(remote)
    }

    /** `intent?.data` is a `Uri?`; the suspend call must take it as is, with no Java shim. */
    @Test
    fun acceptsTheLaunchIntentsNullableUri() = runTest {
        val remote = StubRemoteInterface(200, """{"data":"{\"+clicked_branch_link\":false}"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)
        val launchUri: Uri? = Intent(Intent.ACTION_MAIN).data

        val params = Branch.getInstance().requestDeepLinkData(launchUri)

        assertFalse(params.optBoolean("+clicked_branch_link", true))
        awaitTheLaunchOpen(remote)
    }

    @Test
    fun acceptsANullUri() = runTest {
        val remote = StubRemoteInterface(200, """{"data":"{\"+clicked_branch_link\":false}"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)

        val params = Branch.getInstance().requestDeepLinkData(null)

        assertFalse(params.optBoolean("+clicked_branch_link", true))
        awaitTheLaunchOpen(remote)
    }

    @Test
    fun resolvesTheLinkInTheActivitysLaunchIntent() = runTest {
        val remote = StubRemoteInterface(200, """{"data":"{\"+clicked_branch_link\":true,\"foo\":\"bar\"}"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)
        val activity = Robolectric.buildActivity(Activity::class.java, Intent(Intent.ACTION_VIEW, uri)).get()

        val params = Branch.getInstance().requestDeepLinkData(activity)

        assertEquals("bar", params.optString("foo"))
        awaitTheLaunchOpen(remote)
    }

    @Test
    fun throwsBranchExceptionCarryingTheServerFailure() = runTest {
        val remote = StubRemoteInterface(500, """{"error":"boom"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)

        try {
            Branch.getInstance().requestDeepLinkData(uri)
            fail("expected BranchException when the deep link cannot be resolved")
        } catch (e: BranchException) {
            assertEquals(
                "the BranchError the callback path would have delivered must survive intact",
                500,
                e.branchError.errorCode
            )
        }
        awaitTheLaunchOpen(remote)
    }

    /**
     * Gating the single consumer is what keeps the second request queued long enough for the
     * assertion to mean anything; without it this passes either way.
     */
    @Test
    fun cancellationRemovesTheStillQueuedRequest() = runTest {
        val gated = GatedRemoteInterface()
        Branch.getInstance().setBranchRemoteInterface(gated)

        // Occupy the consumer via the untouched callback API.
        Branch.getInstance().requestDeepLinkData(uri) { _, _ -> }
        assertTrue("the consumer never started the first request", gated.entered.await(10, TimeUnit.SECONDS))

        // Real dispatcher, not runTest's: the polling below blocks this thread.
        val job = launch(Dispatchers.IO) { Branch.getInstance().requestDeepLinkData(uri) }
        try {
            assertTrue("the suspend call never reached the queue", awaitQueueSizeAtLeast(1))

            job.cancel()
            job.join()

            assertEquals(
                "cancelling must remove the still-unsent request from the queue",
                0,
                Branch.getInstance().requestQueue_.getSize()
            )
        } finally {
            // A failed assertion would otherwise leave the consumer parked for the full 10s.
            gated.gate.countDown()
        }
    }

    /** The resolve's open is sent after its callback; left in flight, it lands in the next test's queue. */
    private fun awaitTheLaunchOpen(remote: StubRemoteInterface) {
        assertTrue("the launch's open never went out", remote.openSent.await(10, TimeUnit.SECONDS))
    }

    private fun awaitQueueSizeAtLeast(target: Int): Boolean {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (Branch.getInstance().requestQueue_.getSize() >= target) return true
            Thread.sleep(10)
        }
        return false
    }

    @Test
    fun sendOpen_returnsOnceTheOpenIsSent() = runTest {
        initializeWithAutomaticOpenEventsOff()
        val remote = StubRemoteInterface(200, """{"data":"{\"+clicked_branch_link\":false}"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)
        Branch.getInstance().requestDeepLinkData(uri)

        Branch.getInstance().sendOpen()

        assertTrue("the open must have gone out", remote.openSent.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun sendOpen_throwsBranchExceptionWhenTheOpenFails() = runTest {
        initializeWithAutomaticOpenEventsOff()
        val remote = StubRemoteInterface(500, """{"error":"boom"}""")
        Branch.getInstance().setBranchRemoteInterface(remote)
        runCatching { Branch.getInstance().requestDeepLinkData(uri) }

        try {
            Branch.getInstance().sendOpen()
            fail("a failed open must throw")
        } catch (e: BranchException) {
            assertEquals(500, e.branchError.errorCode)
        }
    }

    private fun initializeWithAutomaticOpenEventsOff() {
        Branch.shutDown()
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").setAutomaticOpenEvents(false).build())
        Branch._userAgentString = "test-agent"
    }
}
