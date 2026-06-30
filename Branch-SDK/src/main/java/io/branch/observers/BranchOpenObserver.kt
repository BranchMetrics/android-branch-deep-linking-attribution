package io.branch.referral

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 *
 */
internal class BranchOpenObserver(private val branchInstance: Branch) : Application.ActivityLifecycleCallbacks {

    private var activityCount = 0
    private var isChangingConfiguration = false

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        BranchLogger.v("BranchOpenObserver onActivityStarted: $activity activityCount incremented to: $activityCount")

        if (activityCount == 1 && !isChangingConfiguration) {
            branchInstance.sendOpen()
        }
        isChangingConfiguration = false
    }

    override fun onActivityStopped(activity: Activity) {
        // A configuration change (e.g. folding/unfolding a foldable) destroys and recreates the
        // foregrounded Activity. Remember it so the following onActivityStarted does not emit a
        // duplicate OPEN for what is still the same session (SDK-2463).
        isChangingConfiguration = activity.isChangingConfigurations
        activityCount--
        BranchLogger.v("BranchOpenObserver onActivityStopped: $activity activityCount decremented to: $activityCount")

        if (activityCount <= 0) {
            activityCount = 0
        }
    }

    // Required overrides for the interface
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        BranchLogger.v("BranchOpenObserver onActivityCreated: $activity activityCount: $activityCount")
    }
    override fun onActivityResumed(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityResumed: $activity activityCount: $activityCount")
    }
    override fun onActivityPaused(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityPaused: $activity activityCount: $activityCount")

    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityDestroyed: $activity activityCount: $activityCount")

    }
}