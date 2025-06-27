package io.branch.referral.modernization.wrappers

import android.app.Activity
import android.content.Context
import io.branch.referral.Branch
import io.branch.referral.modernization.BranchApiPreservationManager
import io.branch.referral.modernization.adapters.CallbackAdapterRegistry
import org.json.JSONObject

/**
 * Static method wrappers for Branch SDK legacy API preservation.
 * 
 * This class provides static wrapper methods that delegate to the modern
 * implementation while maintaining exact API compatibility. All legacy
 * static methods are preserved here with deprecation warnings.
 * 
 * Key features:
 * - Exact method signature preservation
 * - Automatic deprecation warnings
 * - Usage analytics tracking
 * - Seamless delegation to modern core
 */
@Suppress("DEPRECATION")
object PreservedBranchApi {
    
    private val preservationManager = BranchApiPreservationManager.getInstance()
    private val callbackRegistry = CallbackAdapterRegistry.getInstance()
    
    /**
     * Legacy Branch.getInstance() wrapper.
     * Preserves the singleton pattern while delegating to modern core.
     */
    @JvmStatic
    @Deprecated(
        message = "Use ModernBranchCore.getInstance() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance()"),
        level = DeprecationLevel.WARNING
    )
    fun getInstance(): Branch {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getInstance",
            parameters = emptyArray()
        )
        
        // Return wrapped modern implementation as Branch instance
        return LegacyBranchWrapper.getInstance()
    }
    
    /**
     * Legacy Branch.getInstance(Context) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use ModernBranchCore.initialize(Context) instead",
        replaceWith = ReplaceWith("ModernBranchCore.initialize(context)"),
        level = DeprecationLevel.WARNING
    )
    fun getInstance(context: Context): Branch {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getInstance",
            parameters = arrayOf(context)
        )
        
        return LegacyBranchWrapper.getInstance()
    }
    
    /**
     * Legacy Branch.getAutoInstance(Context) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use ModernBranchCore.initialize(Context) instead",
        replaceWith = ReplaceWith("ModernBranchCore.initialize(context)"),
        level = DeprecationLevel.WARNING
    )
    fun getAutoInstance(context: Context): Branch {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getAutoInstance",
            parameters = arrayOf(context)
        )
        
        return LegacyBranchWrapper.getInstance()
    }
    
    /**
     * Legacy Branch.enableTestMode() wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.enableTestMode() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.enableTestMode()"),
        level = DeprecationLevel.WARNING
    )
    fun enableTestMode() {
        preservationManager.handleLegacyApiCall(
            methodName = "enableTestMode",
            parameters = emptyArray()
        )
    }
    
    /**
     * Legacy Branch.enableLogging() wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setDebugMode(true) instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setDebugMode(true)"),
        level = DeprecationLevel.WARNING
    )
    fun enableLogging() {
        preservationManager.handleLegacyApiCall(
            methodName = "enableLogging",
            parameters = emptyArray()
        )
    }
    
    /**
     * Legacy Branch.disableLogging() wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setDebugMode(false) instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setDebugMode(false)"),
        level = DeprecationLevel.WARNING
    )
    fun disableLogging() {
        preservationManager.handleLegacyApiCall(
            methodName = "disableLogging",
            parameters = emptyArray()
        )
    }
    
    /**
     * Legacy Branch.getLatestReferringParamsSync() wrapper.
     * Note: This method is marked for early removal due to its blocking nature.
     */
    @JvmStatic
    @Deprecated(
        message = "Synchronous methods are deprecated. Use dataManager.getLatestReferringParamsAsync() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().dataManager.getLatestReferringParamsAsync()"),
        level = DeprecationLevel.ERROR
    )
    fun getLatestReferringParamsSync(): JSONObject? {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getLatestReferringParamsSync",
            parameters = emptyArray()
        )
        
        return result as? JSONObject
    }
    
    /**
     * Legacy Branch.getFirstReferringParamsSync() wrapper.
     * Note: This method is marked for early removal due to its blocking nature.
     */
    @JvmStatic
    @Deprecated(
        message = "Synchronous methods are deprecated. Use dataManager.getFirstReferringParamsAsync() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().dataManager.getFirstReferringParamsAsync()"),
        level = DeprecationLevel.ERROR
    )
    fun getFirstReferringParamsSync(): JSONObject? {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getFirstReferringParamsSync",
            parameters = emptyArray()
        )
        
        return result as? JSONObject
    }
    
    /**
     * Legacy Branch.isAutoDeepLinkLaunch(Activity) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use sessionManager to check session state instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.isSessionActive()"),
        level = DeprecationLevel.WARNING
    )
    fun isAutoDeepLinkLaunch(activity: Activity): Boolean {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "isAutoDeepLinkLaunch",
            parameters = arrayOf(activity)
        )
        
        return result as? Boolean ?: false
    }
    
    /**
     * Legacy Branch.setBranchKey(String) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setBranchKey() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setBranchKey(key)"),
        level = DeprecationLevel.WARNING
    )
    fun setBranchKey(key: String) {
        preservationManager.handleLegacyApiCall(
            methodName = "setBranchKey",
            parameters = arrayOf(key)
        )
    }
    
    /**
     * Legacy Branch.setRequestTimeout(Int) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setTimeout() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setTimeout(timeout.toLong())"),
        level = DeprecationLevel.WARNING
    )
    fun setRequestTimeout(timeout: Int) {
        preservationManager.handleLegacyApiCall(
            methodName = "setRequestTimeout",
            parameters = arrayOf(timeout)
        )
    }
    
    /**
     * Legacy Branch.setRetryCount(Int) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setRetryCount() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setRetryCount(count)"),
        level = DeprecationLevel.WARNING
    )
    fun setRetryCount(count: Int) {
        preservationManager.handleLegacyApiCall(
            methodName = "setRetryCount",
            parameters = arrayOf(count)
        )
    }
    
    /**
     * Legacy Branch.setRetryInterval(Int) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager.setRetryInterval() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.setRetryInterval(interval)"),
        level = DeprecationLevel.WARNING
    )
    fun setRetryInterval(interval: Int) {
        preservationManager.handleLegacyApiCall(
            methodName = "setRetryInterval",
            parameters = arrayOf(interval)
        )
    }
    
    /**
     * Legacy Branch.sessionBuilder(Activity) wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use sessionManager.initSession() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.initSession(activity)"),
        level = DeprecationLevel.WARNING
    )
    fun sessionBuilder(activity: Activity): SessionBuilder {
        preservationManager.handleLegacyApiCall(
            methodName = "sessionBuilder",
            parameters = arrayOf(activity)
        )
        
        return SessionBuilder(activity)
    }
    
    /**
     * Legacy Branch.getDeepLinkDebugMode() wrapper.
     */
    @JvmStatic
    @Deprecated(
        message = "Use configurationManager to check debug mode instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().configurationManager.isDebugModeEnabled()"),
        level = DeprecationLevel.WARNING
    )
    fun getDeepLinkDebugMode(): JSONObject? {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getDeepLinkDebugMode",
            parameters = emptyArray()
        )
        
        return result as? JSONObject
    }
    
    /**
     * Utility methods for wrapper functionality.
     */
    internal fun handleCallback(callback: Any?, result: Any?, error: Throwable?) {
        callbackRegistry.handleCallback(callback, result, error)
    }
    
    /**
     * Check if the modern core is ready for operations.
     */
    internal fun isModernCoreReady(): Boolean {
        return preservationManager.isReady()
    }
}

/**
 * Legacy SessionBuilder wrapper for maintaining API compatibility.
 */
@Deprecated("Use ModernBranchCore sessionManager instead")
class SessionBuilder(private val activity: Activity) {
    
    private val preservationManager = BranchApiPreservationManager.getInstance()
    
    fun withCallback(callback: Branch.BranchReferralInitListener): SessionBuilder {
        preservationManager.handleLegacyApiCall(
            methodName = "sessionBuilder.withCallback",
            parameters = arrayOf(callback)
        )
        return this
    }
    
    fun withData(data: JSONObject): SessionBuilder {
        preservationManager.handleLegacyApiCall(
            methodName = "sessionBuilder.withData",
            parameters = arrayOf(data)
        )
        return this
    }
    
    fun init(): Boolean {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "sessionBuilder.init",
            parameters = arrayOf(activity)
        )
        return result as? Boolean ?: false
    }
} 