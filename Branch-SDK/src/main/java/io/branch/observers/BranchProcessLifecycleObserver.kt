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
        // Note: closeSessionInternal is still called by BranchActivityLifecycleObserver when the
        // last activity stops. This observer intentionally doesn't duplicate that call to avoid
        // race conditions between process-level and activity-level lifecycle callbacks.
    }

    companion object {
        @Volatile
        private var instance: BranchProcessLifecycleObserver? = null

        /**
         * Registers a single process-lifecycle observer for the given Branch instance. Safe to
         * call more than once (SDK re-init): the previous observer is removed first.
         *
         * **Threading:** If already on main thread, executes synchronously. Otherwise, marshalled
         * to the main thread because ProcessLifecycleOwner requires it. For standard init paths
         * (Application.onCreate on main thread), this is safe and synchronous. Custom integrations
         * calling getAutoInstance() from background threads should ensure the app hasn't entered
         * foreground before registration completes, or the first OPEN event may be missed.
         */
        @JvmStatic
        fun register(branchInstance: Branch) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                registerSync(branchInstance)
            } else {
                Handler(Looper.getMainLooper()).post {
                    registerSync(branchInstance)
                }
            }
        }

        @JvmStatic
        fun unregister() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                unregisterSync()
            } else {
                Handler(Looper.getMainLooper()).post {
                    unregisterSync()
                }
            }
        }

        private fun registerSync(branchInstance: Branch) {
            instance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
            val observer = BranchProcessLifecycleObserver(branchInstance)
            instance = observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }

        private fun unregisterSync() {
            instance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
            instance = null
        }

        /**
         * Shuts down the observer synchronously for test isolation. Blocks the calling thread
         * until unregistration completes on the main thread. Should only be called from test
         * tearDown methods.
         */
        @JvmStatic
        fun shutDownForTesting() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                unregisterSync()
            } else {
                val latch = java.util.concurrent.CountDownLatch(1)
                Handler(Looper.getMainLooper()).post {
                    unregisterSync()
                    latch.countDown()
                }
                try {
                    latch.await(1, java.util.concurrent.TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    BranchLogger.w("shutDownForTesting interrupted: ${e.message}")
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
}
