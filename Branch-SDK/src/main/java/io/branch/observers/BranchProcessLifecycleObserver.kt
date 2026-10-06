package io.branch.referral

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** When the process goes to the background, sends an open the app hasn't sent, then clears `sessionParams` and the saved launch link. */
internal class BranchProcessLifecycleObserver(private val branchInstance: Branch) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        BranchLogger.v("BranchProcessLifecycleObserver onStop: process backgrounded")
        // First, so the clear below waits for that open.
        guarded("sendOpenAtBackground") { branchInstance.sendOpenAtBackground() }
        guarded("onStop") {
            // While attribution is off no open succeeds to clear the saved launch link, so the next launch would carry it.
            if (!branchInstance.requestQueue_.containsDeepLinkOrOpen()) {
                val prefHelper = branchInstance.prefHelper
                prefHelper.sessionParams = PrefHelper.NO_STRING_VALUE
                prefHelper.clearLaunchLink()
            } else {
                branchInstance.launchLinkClearOwed_ = true
            }
        }
    }

    companion object {
        @Volatile
        private var instance: BranchProcessLifecycleObserver? = null

        /** Registers the observer on the main thread, replacing any previous one. */
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
            // Captured before posting, so a concurrent register() is not undone.
            val capturedInstance = instance
            if (Looper.myLooper() == Looper.getMainLooper()) {
                unregisterSync()
            } else {
                Handler(Looper.getMainLooper()).post {
                    guarded("unregister") {
                        capturedInstance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
                    }
                    if (instance === capturedInstance) {
                        instance = null
                    }
                }
            }
        }

        private fun registerSync(branchInstance: Branch) = guarded("register") {
            instance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
            val observer = BranchProcessLifecycleObserver(branchInstance)
            instance = observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }

        private fun unregisterSync() {
            guarded("unregister") {
                instance?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
            }
            instance = null
        }

        // Any failure here only skips that step.
        private inline fun guarded(action: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                BranchLogger.w("BranchProcessLifecycleObserver $action failed: $e")
            } catch (e: LinkageError) {
                BranchLogger.w("BranchProcessLifecycleObserver $action failed: $e")
            }
        }

        /** Unregisters and waits for it to finish. For tests only. */
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
                    if (!latch.await(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        BranchLogger.w("shutDownForTesting timed out — static instance may not be cleaned up")
                    }
                } catch (e: InterruptedException) {
                    BranchLogger.w("shutDownForTesting interrupted: ${e.message}")
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
}
