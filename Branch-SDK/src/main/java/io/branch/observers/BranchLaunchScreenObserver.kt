package io.branch.referral

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.Collections

/**
 * Tells a launch call made by a screen Android rebuilt, after a configuration change or after it ended
 * the app's process, from a new tap: it remembers the links read in saved state, as Jetpack Navigation
 * does. Reads only an intent's data, never its extras.
 */
internal class BranchLaunchScreenObserver : Application.ActivityLifecycleCallbacks {

    private val readLinks: MutableSet<String> = Collections.synchronizedSet(HashSet())

    // A screen rebuilt after its link was read, until its first call or until it resumes.
    private var rebuilt: WeakReference<Activity>? = null

    /** Records the link a launch call read, as passed and as the screen's intent now holds it. */
    fun linkRead(uri: Uri?, activity: Activity?) {
        uri?.let { readLinks.add(it.toString()) }
        activity?.intent?.data?.let { readLinks.add(it.toString()) }
    }

    /** True once, for the first call with the link of a screen Android rebuilt after reading it. */
    @Synchronized
    fun takeRebuildCall(uri: Uri?): Boolean {
        val screen = rebuilt?.get() ?: return false
        if (uri == null || screen.intent?.data != uri) return false
        rebuilt = null
        return true
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        val link = activity.intent?.data ?: return
        if (readLinks.contains(link.toString())) outState.putBoolean(KEY_LINK_USED, true)
    }

    @Synchronized
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        rebuilt = null
        try {
            if (savedInstanceState?.getBoolean(KEY_LINK_USED, false) != true) return
        } catch (e: Exception) {
            BranchLogger.w("BranchLaunchScreenObserver could not read the saved state: $e")
            return
        }
        activity.intent?.data?.let { readLinks.add(it.toString()) }
        rebuilt = WeakReference(activity)
    }

    @Synchronized
    override fun onActivityResumed(activity: Activity) {
        if (rebuilt?.get() === activity) rebuilt = null
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        const val KEY_LINK_USED = "io.branch.sdk.link_used"
    }
}
