package io.branch.referral

import androidx.lifecycle.LifecycleOwner
import io.branch.coroutines.RequestDeepLink
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for [BranchProcessLifecycleObserver].
 *
 * SDK-2463: the OPEN must be emitted when the app PROCESS actually enters the foreground, and
 * not on a configuration-change recreation (fold/unfold, rotation). ProcessLifecycleOwner tracks
 * the whole process, so ON_START does not fire on a config-change recreation. These tests pin the
 * observer's contract (ON_START -> one OPEN, ON_STOP -> no OPEN, one OPEN per real foreground).
 * The config-change property itself is a ProcessLifecycleOwner guarantee, verified on device.
 */
class BranchProcessLifecycleObserverTest : BranchTestBase() {

    private val resolvedPayload = """{"~channel":"Distribution Channel","+clicked_branch_link":true}"""

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
        // Request constructors need DeviceInfo, which initialize provides.
        Branch.initialize(RuntimeEnvironment.getApplication(), BranchConfiguration.Builder("key_live_test123").build())
        val adapter = BranchRequestQueueAdapter.getInstance(RuntimeEnvironment.getApplication())
        Branch::class.java.getDeclaredField("requestQueue_").apply { isAccessible = true }.set(branch, adapter)
        queue = BranchRequestQueueAdapter::class.java.getDeclaredField("newQueue")
            .apply { isAccessible = true }.get(adapter) as BranchRequestQueue
        runBlocking { queue.clear() }
    }

    @After
    fun clearQueue() {
        runBlocking { queue.clear() }
    }

    @Test
    fun processForeground_emitsOpen() {
        observer.onStart(owner)

        verify(branch, times(1)).sendOpen()
    }

    @Test
    fun processBackground_doesNotEmitOpen() {
        observer.onStop(owner)

        verify(branch, never()).sendOpen()
    }

    @Test
    fun backgroundToForeground_emitsOpenOncePerForeground() {
        observer.onStart(owner) // cold foreground
        observer.onStop(owner)  // app backgrounded
        observer.onStart(owner) // returns to foreground

        verify(branch, times(2)).sendOpen()
    }

    @Test
    fun repeatedForegrounds_neverSuppressAnOpen() {
        // The activity-count guard (BranchOpenObserver) can leave a stale isChangingConfigurations
        // flag that suppresses a legitimate OPEN after a config-change stop. This observer holds no
        // per-Activity state: ProcessLifecycleOwner never dispatches ON_START for a config-change
        // recreation, so every ON_START it receives is a real foreground and emits exactly one OPEN.
        observer.onStart(owner)
        observer.onStop(owner)
        observer.onStart(owner)
        observer.onStop(owner)
        observer.onStart(owner)

        verify(branch, times(3)).sendOpen()
    }

    @Test
    fun onStop_clearsResolvedSessionParams() {
        prefHelper.sessionParams = resolvedPayload

        observer.onStop(owner)

        assertEquals(PrefHelper.NO_STRING_VALUE, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsPayloadWhileResolutionQueued() {
        prefHelper.sessionParams = resolvedPayload
        queue.insert(heldRequest(RequestDeepLink(RuntimeEnvironment.getApplication(), null, null, false)), 0)

        observer.onStop(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    // The resolution has written the payload on the main thread but has not left activeRequests yet.
    @Test
    fun onStop_keepsPayloadWhileResolutionExecuting() {
        prefHelper.sessionParams = resolvedPayload
        val resolution = RequestDeepLink(RuntimeEnvironment.getApplication(), null, null, false)
        activeRequests()["RequestDeepLink_executing"] = resolution

        observer.onStop(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    // The resolution has left both collections; only its chained open is still in flight.
    @Test
    fun onStop_keepsPayloadWhileChainedOpenExecuting() {
        prefHelper.sessionParams = resolvedPayload
        val chainedOpen = RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
        activeRequests()["RequestOpen_executing"] = chainedOpen

        observer.onStop(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    // The chained open follows the resolution and its data-less response leaves the slot alone.
    @Test
    fun onStop_keepsPayloadWhileOpenQueued() {
        prefHelper.sessionParams = resolvedPayload
        queue.insert(heldRequest(RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)), 0)

        observer.onStop(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    @Test
    fun onStop_keepsPayloadWhileInstallQueued() {
        prefHelper.sessionParams = resolvedPayload
        queue.insert(heldRequest(ServerRequestRegisterInstall(RuntimeEnvironment.getApplication(), null, false)), 0)

        observer.onStop(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    @Test
    fun onStart_doesNotClearSessionParams() {
        prefHelper.sessionParams = resolvedPayload

        observer.onStart(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    // A wait lock keeps a live processing loop from dequeuing the request mid-test.
    private fun heldRequest(request: ServerRequest): ServerRequest =
        request.apply { addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK) }

    @Suppress("UNCHECKED_CAST")
    private fun activeRequests(): MutableMap<String, ServerRequest> =
        BranchRequestQueue::class.java.getDeclaredField("activeRequests")
            .apply { isAccessible = true }.get(queue) as MutableMap<String, ServerRequest>
}
