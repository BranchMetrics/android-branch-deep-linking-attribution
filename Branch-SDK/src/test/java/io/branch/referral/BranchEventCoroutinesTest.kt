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
import org.junit.Assert.assertEquals
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
        } catch (e: BranchException) {
            assertEquals(BranchError.ERR_BRANCH_TRACKING_DISABLED, e.branchError.errorCode)
        }
    }

    @Test
    fun throwsBranchExceptionCarryingTheServerFailure() = runTest {
        Branch.getInstance().setBranchRemoteInterface(
            StubRemoteInterface(500, """{"error":"Internal server error"}""")
        )

        try {
            BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).awaitLogEvent(context)
            fail("expected the logEvent failure to propagate")
        } catch (e: BranchException) {
            assertEquals(500, e.branchError.errorCode)
        }
    }
}
