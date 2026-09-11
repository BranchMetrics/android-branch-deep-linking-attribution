package io.branch.referral

import androidx.lifecycle.LifecycleOwner
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

    @Before
    override fun setUpBase() {
        super.setUpBase()
        branch = mock(Branch::class.java)
        owner = mock(LifecycleOwner::class.java)
        observer = BranchProcessLifecycleObserver(branch)
        prefHelper = PrefHelper.getInstance(RuntimeEnvironment.getApplication())
        `when`(branch.prefHelper).thenReturn(prefHelper)
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
    fun onStart_doesNotClearSessionParams() {
        prefHelper.sessionParams = resolvedPayload

        observer.onStart(owner)

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }
}
