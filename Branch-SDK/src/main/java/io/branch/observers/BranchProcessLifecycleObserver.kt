package io.branch.referral

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** Clears `sessionParams` when the process goes to the background. Sends no requests. */
internal class BranchProcessLifecycleObserver(private val branchInstance: Branch) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        BranchLogger.v("BranchProcessLifecycleObserver onStop: process backgrounded")
        guarded("onStop") {
            if (!branchInstance.requestQueue_.containsDeepLinkOrOpen()) {
                branchInstance.prefHelper.sessionParams = PrefHelper.NO_STRING_VALUE
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

        // A missing lifecycle-process, or any failure here, only skips the clear.
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
