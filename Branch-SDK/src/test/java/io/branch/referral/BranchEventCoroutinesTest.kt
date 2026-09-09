package io.branch.referral

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.referral.network.BranchRemoteInterface
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
class BranchEventCoroutinesTest : BranchTestBase() {

    private class StubRemoteInterface(
        private val responseCode: Int,
        private val body: String
    ) : BranchRemoteInterface() {
        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse(body, responseCode)
        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse =
            BranchResponse(body, responseCode)
    }

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())
        // v3/events/* needs a live session to send, and an empty user agent would park it
        // behind USER_AGENT_STRING_LOCK.
        Branch._userAgentString = "test-agent"
        Branch.getInstance().prefHelper_.apply {
            setSessionID("test-session-id")
            setRandomizedDeviceToken("test-device-token")
            setRandomizedBundleToken("test-bundle-token")
        }
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
    fun resumesOnceTheEventIsLogged() = runTest {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(200, "{}"))

        BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).awaitLogEvent(context)
    }

    /**
     * Fast-fail profile: rejected before enqueue, so the continuation resumes inside
     * `suspendCancellableCoroutine`'s own block with no dispatch.
     */
    @Test
    fun throwsWhenAttributionIsDisabled() = runTest {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(200, "{}"))
        Branch.getInstance()
            .setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.NONE)

        try {
            BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).awaitLogEvent(context)
            fail("expected the tracking-disabled rejection to propagate")
        } catch (e: Exception) {
            assertTrue(
                "expected the tracking-disabled error code, got: ${e.message}",
                e.message!!.contains(BranchError.ERR_BRANCH_TRACKING_DISABLED.toString())
            )
        }
    }

    /**
     * `onFailure` hands back a raw [Exception], not a [BranchError], so the bridge rethrows
     * as-is. The asymmetry with the other suspend variants is deliberate.
     */
    @Test
    fun rethrowsTheRawCallbackExceptionRatherThanWrappingIt() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(500, """{"error":"boom"}""")
        )

        try {
            BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).awaitLogEvent(context)
            fail("expected the logEvent failure to propagate")
        } catch (e: BranchException) {
            fail("the raw callback exception must not be wrapped in BranchException")
        } catch (e: Exception) {
            assertTrue(
                "expected the callback's own message, got: ${e.message}",
                e.message!!.contains("Failed logEvent server request: 500")
            )
        }
    }
}
