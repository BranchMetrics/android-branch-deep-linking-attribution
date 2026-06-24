package io.branch.referral;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * SDK-2463: BranchOpenObserver (the lifecycle observer actually registered in 6.0 via
 * setActivityLifeCycleObserver) logs an Open whenever the visible-activity count goes 0 -> 1. A
 * configuration change (folding/unfolding a foldable, rotation) destroys and recreates the
 * foregrounded activity, so the count momentarily hits 0 and the recreated activity logs a spurious
 * Open. The observer must skip the Open when the 0 -> 1 transition is a configuration-change
 * recreation, and still fire it on a real foreground.
 */
@RunWith(RobolectricTestRunner.class)
public class BranchOpenObserverConfigChangeTest {

    @Test
    public void configChangeRecreation_doesNotSendSecondOpen() {
        Branch branch = mock(Branch.class);
        Activity activity = mock(Activity.class);
        BranchOpenObserver observer = new BranchOpenObserver(branch);

        observer.onActivityStarted(activity);            // count 0 -> 1: initial open
        when(activity.isChangingConfigurations()).thenReturn(true);
        observer.onActivityStopped(activity);            // count 1 -> 0: config-change recreation
        observer.onActivityStarted(activity);            // count 0 -> 1 again: must NOT open

        verify(branch, times(1)).sendOpen();
    }

    @Test
    public void realBackgroundThenForeground_sendsSecondOpen() {
        Branch branch = mock(Branch.class);
        Activity activity = mock(Activity.class);
        when(activity.isChangingConfigurations()).thenReturn(false);
        BranchOpenObserver observer = new BranchOpenObserver(branch);

        observer.onActivityStarted(activity);            // open #1
        observer.onActivityStopped(activity);            // real background
        observer.onActivityStarted(activity);            // real foreground: open #2

        verify(branch, times(2)).sendOpen();
    }
}
