package io.branch.referral

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 *
 */
internal class BranchOpenObserver(private val branchInstance: Branch) : Application.ActivityLifecycleCallbacks {

    private var activityCount = 0

    // SDK-2463: set when the last visible activity stops because it is being recreated for a
    // configuration change (folding/unfolding a foldable, rotation, etc.). It tells the matching
    // onActivityStarted that the foreground->foreground recreation is not a real app open.
    private var recreatingForConfigChange = false

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        BranchLogger.v("BranchOpenObserver onActivityStarted: $activity activityCount incremented to: $activityCount")

        if (activityCount == 1) {
            if (recreatingForConfigChange) {
                // The single visible activity was just recreated for a configuration change; the app
                // never left the foreground, so logging an Open here would be a spurious open.
                BranchLogger.v("BranchOpenObserver skipping Open: activity recreated for a configuration change (SDK-2463)")
            } else {
                branchInstance.sendOpen()
            }
        }
        recreatingForConfigChange = false
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        BranchLogger.v("BranchOpenObserver onActivityStopped: $activity activityCount decremented to: $activityCount")

        if (activityCount <= 0) {
            activityCount = 0
            // Remember whether this stop is a configuration-change recreation, so the immediately
            // following onActivityStarted does not log a spurious Open.
            recreatingForConfigChange = activity.isChangingConfigurations
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