package io.branch.referral

import android.Manifest
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.coroutines.RequestDeepLink
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

/**
 * EMT-4471 addendum regression tests (defect A): the v3 init path (RequestOpen /
 * RequestDeepLink) never touched the StateFlow-based BranchSessionState, so a successful
 * init left canPerformOperations() / getCurrentSessionState() stuck at Uninitialized forever,
 * even though the legacy getInitState() correctly reported Initialized.
 *
 * BranchRequestQueueAdapter.handleNewRequest now drives Uninitialized -> Initializing before
 * enqueueing any ServerRequestInitSession (A1), and ServerRequestInitSession.onInitSessionFailed
 * resolves a failed init to Failed instead of leaving it in Initializing forever (A2).
 *
 * The request itself completes on a real background dispatcher (BranchRequestQueue's own
 * coroutine scope, not a test dispatcher), so these poll for the terminal state rather than
 * relying on the request's callback: the callback fires before onInitSessionCompleted runs,
 * which is too early to observe the state transition this is testing.
 */
class BranchRequestQueueAdapterSessionStateTest : BranchTestBase() {

    private class StubRemoteInterface(
        private val responseCode: Int,
        private val body: String
    ) : BranchRemoteInterface() {
        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse(body, responseCode)
        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse =
            BranchResponse(body, responseCode)
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

    private fun awaitSessionState(
        timeoutMs: Long = 10_000,
        predicate: (BranchSessionState) -> Boolean
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate(Branch.getInstance().currentSessionState)) return true
            Thread.sleep(10)
        }
        return false
    }

    @Test
    fun requestOpen_completesSuccessfully_leavesSessionAbleToPerformOperations() {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(200, "{}"))
        assertFalse(
            "state must start Uninitialized for this regression to mean anything",
            Branch.getInstance().canPerformOperations()
        )

        Branch.getInstance().requestQueue_.handleNewRequest(RequestOpen(context, null, false, null))

        assertTrue(
            "a completed v3 open must leave canPerformOperations() true, not stuck at Uninitialized",
            awaitSessionState { it is BranchSessionState.Initialized }
        )
        assertTrue(Branch.getInstance().canPerformOperations())
    }

    @Test
    fun requestOpen_serverFailure_transitionsToFailed_notStuckInitializing() {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(500, """{"error":"boom"}"""))

        Branch.getInstance().requestQueue_.handleNewRequest(RequestOpen(context, null, false, null))

        assertTrue(
            "a failed v3 open must resolve to Failed, not stay parked in Initializing",
            awaitSessionState { it is BranchSessionState.Failed }
        )
    }

    @Test
    fun requestDeepLink_completesSuccessfully_leavesSessionAbleToPerformOperations() {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(200, """{"data":"{}"}"""))
        assertFalse(
            "state must start Uninitialized for this regression to mean anything",
            Branch.getInstance().canPerformOperations()
        )

        Branch.getInstance().requestQueue_.handleNewRequest(RequestDeepLink(context, uri, null, false))

        assertTrue(
            "a completed v3 deep link must leave canPerformOperations() true, not stuck at Uninitialized",
            awaitSessionState { it is BranchSessionState.Initialized }
        )
        assertTrue(Branch.getInstance().canPerformOperations())
    }

    @Test
    fun requestDeepLink_serverFailure_transitionsToFailed_notStuckInitializing() {
        Branch.getInstance().setBranchRemoteInterface(StubRemoteInterface(500, """{"error":"boom"}"""))

        Branch.getInstance().requestQueue_.handleNewRequest(RequestDeepLink(context, uri, null, false))

        assertTrue(
            "a failed v3 deep link must resolve to Failed, not stay parked in Initializing",
            awaitSessionState { it is BranchSessionState.Failed }
        )
    }
}
