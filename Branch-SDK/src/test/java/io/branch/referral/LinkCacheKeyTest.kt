package io.branch.referral

import io.branch.referral.network.BranchRemoteInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.ConcurrentHashMap
import java.net.HttpURLConnection

/**
 * Both link generators cache by BranchLinkData, whose hashCode covers the link attributes only.
 * A request carries identifiers that differ on every call, so keying on the serialised payload
 * would make the cache miss every time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkCacheKeyTest : BranchTestBase() {

    @Mock
    private lateinit var remoteInterface: BranchRemoteInterface

    @Mock
    private lateinit var prefHelper: PrefHelper

    private lateinit var generator: ModernLinkGenerator

    @Before
    fun setUpGenerator() {
        `when`(prefHelper.apiBaseUrl).thenReturn("https://api.branch.io/")
        `when`(prefHelper.branchKey).thenReturn("key_live_test")
        generator = ModernLinkGenerator(
            context = RuntimeEnvironment.getApplication(),
            branchRemoteInterface = remoteInterface,
            prefHelper = prefHelper,
            scope = CoroutineScope(testDispatcher + SupervisorJob()),
            defaultTimeoutMs = 5_000L
        )
    }

    @Test
    fun `the same link requested twice reaches the network once`() = runTest {
        `when`(remoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(successResponse(URL))

        val first = linkData().stamped("uuid-1", 1_000L)
        val second = linkData().stamped("uuid-2", 2_000L)

        assertEquals(URL, generator.generateShortLink(first).getOrNull())
        assertEquals(URL, generator.generateShortLink(second).getOrNull())

        verify(remoteInterface, times(1)).make_restful_post(any(), any(), any(), any())
    }

    @Test
    fun `links that differ in an attribute are not shared`() = runTest {
        `when`(remoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(successResponse(URL))

        val email = linkData().stamped("uuid-1", 1_000L)
        val sms = linkData().apply { putChannel("sms") }.stamped("uuid-2", 2_000L)

        generator.generateShortLink(email)
        generator.generateShortLink(sms)

        verify(remoteInterface, times(2)).make_restful_post(any(), any(), any(), any())
    }

    @Test
    fun `the direct path caches under the link attributes`() {
        `when`(remoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(successResponse(URL))
        val legacy = BranchLegacyLinkGenerator(prefHelper, remoteInterface)
        val cache = ConcurrentHashMap<BranchLinkData, String>()

        val url = legacy.generateShortLinkSyncDirect(
            linkData().stamped("uuid-1", 1_000L), false, null, cache
        )

        assertEquals(URL, url)
        // Retrievable by an equivalent link carrying different identifiers -- the property the
        // queue path and Branch.linkCache_ both rely on, and the one toString() keying breaks.
        assertEquals(URL, cache[linkData().stamped("uuid-2", 2_000L)])
    }

    private fun linkData() = BranchLinkData().apply {
        putType(0)
        putAlias("promo")
        putChannel("email")
        putFeature("sharing")
        putStage("new user")
        putDuration(0)
    }

    private fun BranchLinkData.stamped(uuid: String, timestamp: Long) = apply {
        put(Defines.Jsonkey.Branch_Sdk_Request_Uuid.key, uuid)
        put(Defines.Jsonkey.Branch_Sdk_Request_Creation_Time_Stamp.key, timestamp)
    }

    private fun successResponse(url: String) =
        ServerResponse("v1/url", HttpURLConnection.HTTP_OK, "req-1", "Success").apply {
            setPost(JSONObject().put("url", url))
        }

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    private companion object {
        const val URL = "https://test.app.link/abc"
    }
}
