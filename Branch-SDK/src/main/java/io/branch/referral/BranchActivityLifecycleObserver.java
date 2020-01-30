package io.branch.referral;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.branch.indexing.ContentDiscoverer;

/**
 * <p>Class that observes activity life cycle events and determines when to start and stop
 * session.</p>
 */
class BranchActivityLifecycleObserver implements Application.ActivityLifecycleCallbacks {
    private int activityCnt_ = 0; //Keep the count of live  activities.

    /* Flag to find if the activity is launched from stack (incase of single top) or created fresh and launched */
    private boolean isActivityCreatedAndLaunched_ = false;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        branch.setIntentState(Branch.INTENT_STATE.PENDING);
        isActivityCreatedAndLaunched_ = true;
        if (BranchViewHandler.getInstance().isInstallOrOpenBranchViewPending(activity.getApplicationContext())) {
            BranchViewHandler.getInstance().showPendingBranchView(activity);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        branch.setIntentState(Branch.INTENT_STATE.PENDING);
        // If configured on dashboard, trigger content discovery runnable
        if (branch.getInitState() == Branch.SESSION_STATE.INITIALISED) {
            try {
                ContentDiscoverer.getInstance().discoverContent(activity, branch.getSessionReferredLink());
            } catch (Exception ignore) {
            }
        }
        activityCnt_++;
        isActivityCreatedAndLaunched_ = false;

        maybeRefreshAdvertisingID(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        branch.currentActivityReference_ = new WeakReference<>(activity);

        // if the intent state is bypassed from the last activity as it was closed before onResume, we need to skip this with the current
        // activity also to make sure we do not override the intent data
        if (!Branch.bypassCurrentActivityIntentState()) {
            branch.setIntentState(Branch.INTENT_STATE.READY);
            // Grab the intent only for first activity unless this activity is intent to  force new session
            boolean grabIntentParams = activity.getIntent() != null &&
                    branch.getInitState() != Branch.SESSION_STATE.INITIALISED;
            branch.onIntentReady(activity, grabIntentParams);
        }

        if (branch.getInitState() == Branch.SESSION_STATE.UNINITIALISED) {
            if (BranchUtil.getPluginType() == null) {
                // this is the only place where we self-initialize in case user opens the app from 'recent apps tray'
                // and the entry Activity is not the launcher Activity where user placed initSession themselves.
                PrefHelper.Debug("initializing session on user's behalf (onActivityResumed called but SESSION_STATE = UNINITIALISED)");
                branch.initSession(activity);
            } else {
                PrefHelper.Debug("onActivityResumed called and SESSION_STATE = UNINITIALISED, however this is a " + BranchUtil.getPluginType() + " plugin, so we are NOT initializing session on user's behalf");
            }
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        /* Close any opened sharing dialog.*/
        if (branch.getShareLinkManager() != null) {
            branch.getShareLinkManager().cancelShareLinkDialog(true);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        ContentDiscoverer.getInstance().onActivityStopped(activity);
        activityCnt_--; // Check if this is the last activity. If so, stop the session.
        if (activityCnt_ < 1) {
            branch.setInstantDeepLinkPossible(false);
            branch.closeSessionInternal();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        if (branch.currentActivityReference_ != null && branch.currentActivityReference_.get() == activity) {
            branch.currentActivityReference_.clear();
        }
        BranchViewHandler.getInstance().onCurrentActivityDestroyed(activity);
    }

    private void maybeRefreshAdvertisingID(Context context) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        boolean fullyInitialized = branch.getTrackingController() != null &&
                branch.getDeviceInfo() != null && branch.getDeviceInfo().getSystemObserver() != null &&
                branch.getPrefHelper() != null && branch.getPrefHelper().getSessionID() != null;
        if (!fullyInitialized) return;

        final String AIDInitializationSessionID = branch.getDeviceInfo().getSystemObserver().getAIDInitializationSessionID();
        boolean AIDInitializedInThisSession = branch.getPrefHelper().getSessionID().equals(AIDInitializationSessionID);

        if (!AIDInitializedInThisSession && !branch.isGAParamsFetchInProgress() && !branch.getTrackingController().isTrackingDisabled()) {
            branch.setGAParamsFetchInProgress(branch.getDeviceInfo().getSystemObserver().prefetchAdsParams(context, branch));
        }
    }

    boolean isActivityCreatedAndLaunched() {
        return isActivityCreatedAndLaunched_;
    }
}