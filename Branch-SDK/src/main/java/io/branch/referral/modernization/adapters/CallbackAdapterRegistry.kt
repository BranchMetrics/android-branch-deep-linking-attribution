package io.branch.referral.modernization.adapters

import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Callback adapter registry for maintaining interface compatibility.
 * 
 * This system provides adapters between legacy callback interfaces and
 * the modern async/reactive architecture, ensuring seamless operation
 * during the transition period.
 * 
 * Key features:
 * - Complete callback interface preservation
 * - Async-to-sync adaptation when needed
 * - Error handling and logging
 * - Thread-safe operations
 */
class CallbackAdapterRegistry private constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    companion object {
        @Volatile
        private var instance: CallbackAdapterRegistry? = null
        
        fun getInstance(): CallbackAdapterRegistry {
            return instance ?: synchronized(this) {
                instance ?: CallbackAdapterRegistry().also { instance = it }
            }
        }
    }
    
    /**
     * Generic callback handler for all legacy callbacks.
     */
    fun handleCallback(callback: Any?, result: Any?, error: Throwable?) {
        when (callback) {
            is Branch.BranchReferralInitListener -> adaptInitSessionCallback(callback, result, error)
            is Branch.BranchLinkCreateListener -> adaptLinkCreateCallback(callback, result, error)
            else -> {
                BranchLogger.w("Unknown callback type: ${callback?.javaClass?.simpleName}")
            }
        }
    }
    
    /**
     * Adapt init session callbacks from modern async results.
     */
    fun adaptInitSessionCallback(
        callback: Branch.BranchReferralInitListener,
        result: Any?,
        error: Throwable?
    ) {
        scope.launch {
            try {
                if (error != null) {
                    val branchError = convertToBranchError(error)
                    callback.onInitFinished(null, branchError)
                } else {
                    val referringParams = result as? JSONObject ?: JSONObject()
                    callback.onInitFinished(referringParams, null)
                }
            } catch (e: Exception) {
                BranchLogger.e("Error in init session callback adaptation: ${e.message}")
                                 callback.onInitFinished(null, BranchError("Callback adaptation error", -1001))
            }
        }
    }
    
    /**
     * Adapt identity-related callbacks.
     */
    fun adaptIdentityCallback(
        callback: Branch.BranchReferralInitListener,
        result: Any?,
        error: Throwable?
    ) {
        scope.launch {
            try {
                if (error != null) {
                    callback.onInitFinished(null, convertToBranchError(error))
                } else {
                    val userData = result as? JSONObject ?: JSONObject()
                    callback.onInitFinished(userData, null)
                }
            } catch (e: Exception) {
                BranchLogger.e("Error in identity callback adaptation: ${e.message}")
                                 callback.onInitFinished(null, BranchError("Identity callback error", -1002))
            }
        }
    }
    

    
    /**
     * Adapt link creation callbacks.
     */
    fun adaptLinkCreateCallback(
        callback: Branch.BranchLinkCreateListener,
        result: Any?,
        error: Throwable?
    ) {
        scope.launch {
            try {
                if (error != null) {
                    callback.onLinkCreate(null, convertToBranchError(error))
                } else {
                    val url = result as? String ?: ""
                    callback.onLinkCreate(url, null)
                }
            } catch (e: Exception) {
                BranchLogger.e("Error in link create callback adaptation: ${e.message}")
                                 callback.onLinkCreate(null, BranchError("Link creation error", -1004))
            }
        }
    }
    

    

    

    

    

    
    /**
     * Convert modern exceptions to legacy BranchError format.
     */
    private fun convertToBranchError(error: Throwable): BranchError {
                 return when (error) {
             is IllegalArgumentException -> BranchError(
                 error.message ?: "Invalid parameter",
                 -2001
             )
             is SecurityException -> BranchError(
                 error.message ?: "Security error",
                 -2002
             )
             is IllegalStateException -> BranchError(
                 error.message ?: "Invalid state",
                 -2003
             )
             is kotlinx.coroutines.TimeoutCancellationException -> BranchError(
                 "Request timeout",
                 -2004
             )
             is java.net.UnknownHostException -> BranchError(
                 "Network error: ${error.message}",
                 -2005
             )
             is java.io.IOException -> BranchError(
                 "IO error: ${error.message}",
                 -2006
             )
             else -> BranchError(
                 error.message ?: "Unknown error",
                 -2007
             )
         }
    }
    
    /**
     * Utility method to run callbacks on the main thread.
     */
    private fun runOnMainThread(action: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            try {
                action()
            } catch (e: Exception) {
                BranchLogger.e("Error running callback on main thread: ${e.message}")
            }
        }
    }
    
    /**
     * Clean up resources when no longer needed.
     */
    fun cleanup() {
        scope.cancel()
    }
    
         /**
      * Create a timeout-safe callback wrapper.
      */
     private fun <T> withTimeoutWrapper(
         timeoutMs: Long = 30000, // 30 seconds default
         callback: suspend () -> T
     ): Deferred<T> {
         return scope.async {
             withTimeout(timeoutMs) {
                 callback()
             }
         }
     }
    
    /**
     * Handle batch callback operations.
     */
    fun handleBatchCallbacks(
        callbacks: List<Pair<Any?, Any?>>,
        results: List<Any?>,
        errors: List<Throwable?>
    ) {
        callbacks.forEachIndexed { index, (callback, _) ->
            val result = results.getOrNull(index)
            val error = errors.getOrNull(index)
            handleCallback(callback, result, error)
        }
    }
    
    /**
     * Get adapter statistics for monitoring.
     */
    fun getAdapterStats(): AdapterStats {
        return AdapterStats(
            totalCallbacks = 0, // Would track in production
            successfulAdaptations = 0,
            failedAdaptations = 0,
            averageAdaptationTimeMs = 0.0
        )
    }
}

/**
 * Statistics for callback adapter performance monitoring.
 */
data class AdapterStats(
    val totalCallbacks: Int,
    val successfulAdaptations: Int,
    val failedAdaptations: Int,
    val averageAdaptationTimeMs: Double
) 