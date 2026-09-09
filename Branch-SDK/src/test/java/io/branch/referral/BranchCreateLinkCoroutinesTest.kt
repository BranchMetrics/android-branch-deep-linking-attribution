package io.branch.referral

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.indexing.BranchUniversalObject
import io.branch.indexing.createLink
import io.branch.referral.network.BranchRemoteInterface
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
class BranchCreateLinkCoroutinesTest : BranchTestBase() {

    private class StubRemoteInterface(
        private val responseCode: Int,
        private val body: String
    ) : BranchRemoteInterface() {
        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse(body, responseCode)
        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse =
            BranchResponse(body, responseCode)
    }

    private val context get() = RuntimeEnvironment.getApplication()
    private val buo get() = BranchUniversalObject().setCanonicalIdentifier("item/1")
    private val linkProperties
        get() = LinkProperties().setChannel("create-link").setFeature("sharing")

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
    fun returnsTheGeneratedShortUrl() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(200, """{"url":"https://example.app.link/short"}""")
        )

        val url = buo.createLink(context, linkProperties)

        assertEquals("https://example.app.link/short", url)
    }

    @Test
    fun fallsBackToTheLongUrlWhenDefaultToLongUrlIsTrue() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(500, """{"error":"boom"}""")
        )

        val url = buo.createLink(context, linkProperties, defaultToLongUrl = true)

        assertTrue("expected a long URL fallback, got: $url", url.contains("key_live_test123"))
        assertTrue("the long URL must carry the link properties, got: $url",
            url.contains("channel=create-link"))
    }

    @Test
    fun throwsWhenDefaultToLongUrlIsFalse() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(500, """{"error":"boom"}""")
        )

        try {
            buo.createLink(context, linkProperties, defaultToLongUrl = false)
            fail("expected BranchException when no URL is available")
        } catch (e: BranchException) {
            assertEquals(
                "the BranchError the callback path would have delivered must survive intact",
                BranchError.ERR_BRANCH_UNABLE_TO_REACH_SERVERS,
                e.branchError.errorCode
            )
        }
    }

    @Test
    fun builderLevelCreateLinkReturnsTheShortUrl() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(200, """{"url":"https://example.app.link/frombuilder"}""")
        )

        val url = BranchShortLinkBuilder(context)
            .setChannel("create-link")
            .setFeature("sharing")
            .createLink()

        assertEquals("https://example.app.link/frombuilder", url)
    }
}
