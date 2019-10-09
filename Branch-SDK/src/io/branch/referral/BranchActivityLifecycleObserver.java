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

    /* Flag to find if the activity is launched from stack (incase of  single top) or created fresh and launched */
    private boolean isActivityCreatedAndLaunched = false;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        branch.setIntentState(Branch.INTENT_STATE.PENDING);
        isActivityCreatedAndLaunched = true;
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
        if (activityCnt_ < 1) { // Check if this is the first Activity.If so start a session.
            if (branch.getInitState() == Branch.SESSION_STATE.INITIALISED) {
                // Handling case :  init session completed previously when app was in background.
                branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
            }
            branch.startSession(activity);
        } else if (branch.checkIntentForSessionRestart(activity.getIntent())) { // Case of opening the app by clicking a push notification while app is in foreground
            branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
            // no need call close here since it is session forced restart. Don't want to wait till close finish
            branch.startSession(activity);
        }
        activityCnt_++;
        isActivityCreatedAndLaunched = false;

        maybeRefreshAdvertisingID(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Branch branch = Branch.getInstance();
        if (branch == null) return;

        // Need to check here again for session restart request in case the intent is created while the activity is already running
        if (branch.checkIntentForSessionRestart(activity.getIntent())) {
            branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
            branch.startSession(activity);
        }
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
        return isActivityCreatedAndLaunched;
    }
}