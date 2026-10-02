package io.branch.referral

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.coroutines.RequestDeepLink
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

class BranchProcessLifecycleObserverTest : BranchTestBase() {

    private val linkSessionParams = """{"~channel":"Distribution Channel","+clicked_branch_link":true}"""

    private lateinit var branch: Branch
    private lateinit var owner: LifecycleOwner
    private lateinit var observer: BranchProcessLifecycleObserver
    private lateinit var prefHelper: PrefHelper
    private lateinit var queue: BranchRequestQueue

    @Before
    override fun setUpBase() {
        super.setUpBase()
        branch = mock(Branch::class.java)
        owner = mock(LifecycleOwner::class.java)
        observer = BranchProcessLifecycleObserver(branch)
        prefHelper = PrefHelper.getInstance(RuntimeEnvironment.getApplication())
        `when`(branch.prefHelper).thenReturn(prefHelper)
        Branch.initialize(RuntimeEnvironment.getApplication(), BranchConfiguration.Builder("key_live_test123").build())
        val adapter = BranchRequestQueueAdapter.getInstance(RuntimeEnvironment.getApplication())
        Branch::class.java.getDeclaredField("requestQueue_").apply { isAccessible = true }.set(branch, adapter)
        queue = BranchRequestQueueAdapter::class.java.getDeclaredField("newQueue")
            .apply { isAccessible = true }.get(adapter) as BranchRequestQueue
        runBlocking { queue.clear() }
    }

    @After
    override fun tearDownBase() {
        runBlocking { queue.clear() }
        setProcessState(Lifecycle.State.CREATED)
        Branch.shutDown()
        super.tearDownBase()
    }

    @Test
    fun onStop_clearsSessionParams() {
        prefHelper.sessionParams = linkSessionParams

        observer.onStop(owner)

        assertEquals(PrefHelper.NO_STRING_VALUE, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsSessionParamsWhileDeepLinkQueued() {
        prefHelper.sessionParams = linkSessionParams
        queue.insert(heldRequest(RequestDeepLink(RuntimeEnvironment.getApplication(), null, null, false)), 0)

        observer.onStop(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsSessionParamsWhileDeepLinkExecuting() {
        prefHelper.sessionParams = linkSessionParams
        val deepLink = RequestDeepLink(RuntimeEnvironment.getApplication(), null, null, false)
        activeRequests()["RequestDeepLink_executing"] = deepLink

        observer.onStop(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsSessionParamsWhileOpenExecuting() {
        prefHelper.sessionParams = linkSessionParams
        val open = RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
        activeRequests()["RequestOpen_executing"] = open

        observer.onStop(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsSessionParamsWhileOpenQueued() {
        prefHelper.sessionParams = linkSessionParams
        queue.insert(heldRequest(RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)), 0)

        observer.onStop(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsSessionParamsWhileInstallQueued() {
        prefHelper.sessionParams = linkSessionParams
        queue.insert(heldRequest(ServerRequestRegisterInstall(RuntimeEnvironment.getApplication(), null, false)), 0)

        observer.onStop(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun onStop_failure_doesNotThrow() {
        `when`(branch.prefHelper).thenThrow(IllegalStateException("prefs unavailable"))

        observer.onStop(owner)
    }

    @Test
    fun foregroundAndBackground_sendNoRequest() {
        observer.onStart(owner)
        observer.onStop(owner)
        observer.onStart(owner)

        assertEquals(0, queue.getSize())
    }

    @Test
    fun onStart_doesNotClearSessionParams() {
        prefHelper.sessionParams = linkSessionParams

        observer.onStart(owner)

        assertEquals(linkSessionParams, prefHelper.sessionParams)
    }

    @Test
    fun processBackground_afterInitialize_clearsSessionParams() {
        setProcessState(Lifecycle.State.RESUMED)
        prefHelper.sessionParams = linkSessionParams

        setProcessState(Lifecycle.State.CREATED)

        assertEquals(PrefHelper.NO_STRING_VALUE, prefHelper.sessionParams)
    }

    private fun setProcessState(state: Lifecycle.State) {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState = state
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    // Keeps the queue from sending it mid-test.
    private fun heldRequest(request: ServerRequest): ServerRequest =
        request.apply { addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK) }

    @Suppress("UNCHECKED_CAST")
    private fun activeRequests(): MutableMap<String, ServerRequest> =
        BranchRequestQueue::class.java.getDeclaredField("activeRequests")
            .apply { isAccessible = true }.get(queue) as MutableMap<String, ServerRequest>
}
