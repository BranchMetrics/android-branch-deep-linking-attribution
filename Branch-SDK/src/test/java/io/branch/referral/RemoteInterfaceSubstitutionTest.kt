package io.branch.referral

import io.branch.referral.network.BranchRemoteInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import java.net.HttpURLConnection

/**
 * A remote interface installed through [BranchConfiguration.Builder.setRemoteInterface] must
 * reach link generation, not only the request queue.
 *
 * The queue resolves the interface per request, so substitution has always worked there. Both
 * link generators instead receive it in the Branch constructor, which runs before the configured
 * one is installed, so they keep using the real transport.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteInterfaceSubstitutionTest : BranchTestBase() {

    @Mock
    private lateinit var remoteInterface: BranchRemoteInterface

    @Before
    fun initialiseWithConfiguredInterface() {
        // A singleton leaked by an earlier class would make initialize() below a no-op, per
        // Branch.java:378-382, silently skipping the mock installation.
        Branch.shutDown()
        `when`(remoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(successResponse())
        Branch.initialize(
            RuntimeEnvironment.getApplication(),
            BranchConfiguration.Builder(BRANCH_KEY)
                .setRemoteInterface(remoteInterface)
                // Isolates link generation. An automatic open would reach the mock through the
                // queue, which resolves late and already works, and would pass either way.
                .setAutomaticOpenEvents(false)
                .build()
        )
    }

    @After
    fun resetSingleton() {
        Branch.shutDown()
    }

    @Test
    fun `the async link path reaches the configured remote interface`() {
        Branch.getInstance().generateShortLinkInternal(createUrlRequest(async = true, alias = "async"))

        verify(remoteInterface, timeout(ASYNC_TIMEOUT_MS))
            .make_restful_post(any(), any(), eq(Defines.RequestPath.GetURL.path), any())
    }

    @Test
    fun `the sync link path reaches the configured remote interface`() {
        Branch.getInstance().generateShortLinkInternal(createUrlRequest(async = false, alias = "sync"))

        verify(remoteInterface)
            .make_restful_post(any(), any(), eq(Defines.RequestPath.GetURL.path), any())
    }

    /** The alias differs per test so neither shares a [Branch.linkCache_] entry with the other. */
    private fun createUrlRequest(async: Boolean, alias: String): ServerRequestCreateUrl {
        val request = mock(ServerRequestCreateUrl::class.java)
        `when`(request.linkPost).thenReturn(linkData(alias))
        `when`(request.isAsync).thenReturn(async)
        `when`(request.isDefaultToLongUrl).thenReturn(false)
        return request
    }

    private fun linkData(alias: String) = BranchLinkData().apply {
        putType(0)
        putAlias(alias)
        putChannel("email")
        putFeature("sharing")
        putStage("new user")
        putDuration(0)
    }

    private fun successResponse() =
        ServerResponse(Defines.RequestPath.GetURL.path, HttpURLConnection.HTTP_OK, "req-1", "Success")
            .apply { setPost(JSONObject().put("url", SHORT_URL)) }

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    private fun eq(value: String): String = org.mockito.ArgumentMatchers.eq(value) ?: value

    private companion object {
        const val BRANCH_KEY = "key_live_testing_only"
        const val SHORT_URL = "https://test.app.link/abc"
        const val ASYNC_TIMEOUT_MS = 5_000L
    }
}
