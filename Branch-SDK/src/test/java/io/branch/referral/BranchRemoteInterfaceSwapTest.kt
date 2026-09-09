package io.branch.referral

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Holds [Branch.setBranchRemoteInterface] to its documented test-seam contract for link
 * creation, which posts via [ModernLinkGenerator] rather than `branchRemoteInterface_`.
 */
class BranchRemoteInterfaceSwapTest : BranchTestBase() {

    private class StubRemoteInterface(private val shortUrl: String) : BranchRemoteInterface() {
        var postCount = 0

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            postCount++
            return BranchResponse("""{"url":"$shortUrl"}""", 200)
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState =
            Lifecycle.State.CREATED
    }

    @Test
    fun syncLinkCreationUsesTheSwappedRemoteInterface() {
        val stub = StubRemoteInterface("https://example.app.link/sync")
        Branch.getInstance().setBranchRemoteInterface(stub)

        val url = BranchShortLinkBuilder(context)
            .setChannel("swap-sync")
            .getShortUrl()

        assertEquals("https://example.app.link/sync", url)
        assertEquals("the swapped interface must be the one that posts", 1, stub.postCount)
    }

    @Test
    fun asyncLinkCreationUsesTheSwappedRemoteInterface() {
        val stub = StubRemoteInterface("https://example.app.link/async")
        Branch.getInstance().setBranchRemoteInterface(stub)

        val done = CountDownLatch(1)
        var createdUrl: String? = null
        var createError: BranchError? = null

        BranchShortLinkBuilder(context)
            .setChannel("swap-async")
            .generateShortUrl { url, error ->
                createdUrl = url
                createError = error
                done.countDown()
            }

        done.await(10, TimeUnit.SECONDS)
        runMainLooperTasks()

        assertNull(createError)
        assertEquals("https://example.app.link/async", createdUrl)
        assertEquals("the swapped interface must be the one that posts", 1, stub.postCount)
    }
}
