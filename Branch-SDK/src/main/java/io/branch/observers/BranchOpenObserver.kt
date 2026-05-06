package io.branch.referral

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 *
 */
internal class BranchOpenObserver(private val branchInstance: Branch) : Application.ActivityLifecycleCallbacks {

    private var activityCount = 0

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        BranchLogger.v("BranchOpenObserver onActivityStarted: " + activity + " activityCount incremented to: " + activityCount)

        if (activityCount == 1) {
            branchInstance.sendOpen()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        BranchLogger.v("BranchOpenObserver onActivityStarted: " + activity + " activityCount decremented to: " + activityCount)

        if (activityCount <= 0) {
            activityCount = 0
            // No more close event.
        }
    }

    // Required overrides for the interface
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        BranchLogger.v("BranchOpenObserver onActivityCreated: " + activity + " activityCount: " + activityCount)
    }
    override fun onActivityResumed(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityResumed: " + activity + " activityCount: " + activityCount)
    }
    override fun onActivityPaused(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityPaused: " + activity + " activityCount: " + activityCount)

    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        BranchLogger.v("BranchOpenObserver onActivityDestroyed: " + activity + " activityCount: " + activityCount)

    }
}