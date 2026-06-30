package io.branch.referral

import android.app.Activity
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [BranchOpenObserver].
 *
 * Covers SDK-2463: folding/unfolding a foldable device triggers a configuration change that
 * recreates the foregrounded Activity. The observer must not treat that recreation as a fresh
 * foreground and emit a duplicate OPEN, while a genuine background-to-foreground still does.
 */
class BranchOpenObserverTest : BranchTestBase() {

    private lateinit var branch: Branch
    private lateinit var activity: Activity
    private lateinit var observer: BranchOpenObserver

    @Before
    fun setUp() {
        super.setUpBase()
        branch = mock(Branch::class.java)
        activity = mock(Activity::class.java)
        observer = BranchOpenObserver(branch)
    }

    @Test
    fun foldUnfoldWhileForegrounded_doesNotEmitDuplicateOpen() {
        // App enters the foreground: exactly one OPEN.
        observer.onActivityStarted(activity)

        // Fold/unfold recreates the foregrounded Activity via a configuration change.
        `when`(activity.isChangingConfigurations).thenReturn(true)
        observer.onActivityStopped(activity)
        observer.onActivityStarted(activity)

        verify(branch, times(1)).sendOpen()
    }

    @Test
    fun genuineBackgroundToForeground_emitsOpenAgain() {
        observer.onActivityStarted(activity)

        // Real background (not a configuration change), then return to foreground.
        `when`(activity.isChangingConfigurations).thenReturn(false)
        observer.onActivityStopped(activity)
        observer.onActivityStarted(activity)

        verify(branch, times(2)).sendOpen()
    }
}
