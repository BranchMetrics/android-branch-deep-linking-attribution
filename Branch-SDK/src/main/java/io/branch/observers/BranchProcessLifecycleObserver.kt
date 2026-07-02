package io.branch.referral

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Detects app foreground/background at the PROCESS level using AndroidX ProcessLifecycleOwner.
 *
 * Unlike per-Activity start/stop counting, `ProcessLifecycleOwner` ON_START fires only when the
 * whole process actually enters the foreground (cold start or return from background), and NOT on
 * a configuration-change recreation (fold/unfold, rotation, multi-window) or on navigation between
 * Activities. That removes the duplicate OPEN on foldable configuration changes (SDK-2463) by
 * construction, without any manual counting or shared flags.
 */
internal class BranchProcessLifecycleObserver(private val branchInstance: Branch) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        BranchLogger.v("BranchProcessLifecycleObserver onStart: process foregrounded, sending OPEN")
        branchInstance.sendOpen()
    }

    override fun onStop(owner: LifecycleOwner) {
        BranchLogger.v("BranchProcessLifecycleObserver onStop: process backgrounded")
    }

    companion object {
        private var instance: BranchProcessLifecycleObserver? = null

        /**
         * Registers a single process-lifecycle observer for the given Branch instance. Safe to
         * call more than once (SDK re-init): the previous observer is removed first. Registration
         * is marshalled to the main thread because ProcessLifecycleOwner requires it.
         */
        @JvmStatic
        fun register(branchInstance: Branch) {
            Handler(Looper.getMainLooper()).post {
                instance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
                val observer = BranchProcessLifecycleObserver(branchInstance)
                instance = observer
                ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            }
        }
    }
}
