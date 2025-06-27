package io.branch.referral.modernization.wrappers

import android.app.Activity
import android.content.Context
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.modernization.BranchApiPreservationManager
import io.branch.referral.modernization.adapters.CallbackAdapterRegistry
import org.json.JSONObject

/**
 * Legacy Branch wrapper for API preservation during modernization.
 * 
 * This class provides a compatibility layer that maintains all legacy
 * API methods while delegating to the modern implementation. It serves
 * as a bridge during the transition period.
 * 
 * Key features:
 * - Complete API surface preservation
 * - Automatic usage tracking and analytics
 * - Seamless delegation to modern architecture
 * - Thread-safe operation
 * - Deprecation warnings and migration guidance
 */
@Suppress("DEPRECATION")
class LegacyBranchWrapper private constructor() {
    
    private val preservationManager = BranchApiPreservationManager.getInstance()
    private val callbackRegistry = CallbackAdapterRegistry.getInstance()
    
    companion object {
        @Volatile
        private var instance: LegacyBranchWrapper? = null
        
        fun getInstance(): LegacyBranchWrapper {
            return instance ?: synchronized(this) {
                instance ?: LegacyBranchWrapper().also { instance = it }
            }
        }
    }
    
    /**
     * Legacy initSession wrapper with activity.
     */
    @Deprecated(
        message = "Use sessionManager.initSession() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.initSession(activity)"),
        level = DeprecationLevel.WARNING
    )
    fun initSession(activity: Activity): Boolean {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "initSession",
            parameters = arrayOf(activity)
        )
        return result as? Boolean ?: false
    }
    
    /**
     * Legacy initSession wrapper with callback.
     */
    @Deprecated(
        message = "Use sessionManager.initSession() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.initSession(activity)"),
        level = DeprecationLevel.WARNING
    )
    fun initSession(
        callback: Branch.BranchReferralInitListener?,
        activity: Activity
    ): Boolean {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "initSession",
            parameters = arrayOf(callback, activity)
        )
        
        // Handle callback asynchronously
        if (callback != null) {
            callbackRegistry.adaptInitSessionCallback(callback, result, null)
        }
        
        return result as? Boolean ?: false
    }
    
    /**
     * Legacy initSession wrapper with URI data.
     */
    @Deprecated(
        message = "Use sessionManager.initSession() with link data instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.initSession(activity)"),
        level = DeprecationLevel.WARNING
    )
    fun initSession(
        callback: Branch.BranchReferralInitListener?,
        data: android.net.Uri?,
        activity: Activity
    ): Boolean {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "initSession",
            parameters = arrayOf(callback, data, activity)
        )
        
        if (callback != null) {
            callbackRegistry.adaptInitSessionCallback(callback, result, null)
        }
        
        return result as? Boolean ?: false
    }
    
    /**
     * Legacy setIdentity wrapper.
     */
    @Deprecated(
        message = "Use identityManager.setIdentity() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().identityManager.setIdentity(userId)"),
        level = DeprecationLevel.WARNING
    )
    fun setIdentity(userId: String) {
        preservationManager.handleLegacyApiCall(
            methodName = "setIdentity",
            parameters = arrayOf(userId)
        )
    }
    
    /**
     * Legacy setIdentity wrapper with callback.
     */
    @Deprecated(
        message = "Use identityManager.setIdentity() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().identityManager.setIdentity(userId)"),
        level = DeprecationLevel.WARNING
    )
    fun setIdentity(
        userId: String,
        callback: Branch.BranchReferralInitListener?
    ) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "setIdentity",
            parameters = arrayOf(userId, callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptIdentityCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy logout wrapper.
     */
    @Deprecated(
        message = "Use identityManager.logout() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().identityManager.logout()"),
        level = DeprecationLevel.WARNING
    )
    fun logout() {
        preservationManager.handleLegacyApiCall(
            methodName = "logout",
            parameters = emptyArray()
        )
    }
    
    /**
     * Legacy logout wrapper with callback.
     */
    @Deprecated(
        message = "Use identityManager.logout() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().identityManager.logout()"),
        level = DeprecationLevel.WARNING
    )
    fun logout(callback: Branch.BranchReferralStateChangedListener?) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "logout",
            parameters = arrayOf(callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptLogoutCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy resetUserSession wrapper.
     */
    @Deprecated(
        message = "Use sessionManager.resetSession() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().sessionManager.resetSession()"),
        level = DeprecationLevel.WARNING
    )
    fun resetUserSession() {
        preservationManager.handleLegacyApiCall(
            methodName = "resetUserSession",
            parameters = emptyArray()
        )
    }
    
    /**
     * Legacy getFirstReferringParams wrapper.
     */
    @Deprecated(
        message = "Use dataManager.getFirstReferringParams() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().dataManager.getFirstReferringParams()"),
        level = DeprecationLevel.WARNING
    )
    fun getFirstReferringParams(): JSONObject? {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getFirstReferringParams",
            parameters = emptyArray()
        )
        return result as? JSONObject
    }
    
    /**
     * Legacy getLatestReferringParams wrapper.
     */
    @Deprecated(
        message = "Use dataManager.getLatestReferringParams() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().dataManager.getLatestReferringParams()"),
        level = DeprecationLevel.WARNING
    )
    fun getLatestReferringParams(): JSONObject? {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getLatestReferringParams",
            parameters = emptyArray()
        )
        return result as? JSONObject
    }
    
    /**
     * Legacy generateShortUrl wrapper.
     */
    @Deprecated(
        message = "Use linkManager.createShortLink() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().linkManager.createShortLink(linkData)"),
        level = DeprecationLevel.WARNING
    )
    fun generateShortUrl(
        linkData: Map<String, Any>,
        callback: Branch.BranchLinkCreateListener
    ) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "generateShortUrl",
            parameters = arrayOf(linkData, callback)
        )
        
        callbackRegistry.adaptLinkCreateCallback(callback, result, null)
    }
    
    /**
     * Legacy userCompletedAction wrapper.
     */
    @Deprecated(
        message = "Use eventManager.logEvent() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().eventManager.logEvent(eventName, properties)"),
        level = DeprecationLevel.WARNING
    )
    fun userCompletedAction(action: String) {
        preservationManager.handleLegacyApiCall(
            methodName = "userCompletedAction",
            parameters = arrayOf(action)
        )
    }
    
    /**
     * Legacy userCompletedAction wrapper with metadata.
     */
    @Deprecated(
        message = "Use eventManager.logEvent() instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().eventManager.logEvent(eventName, properties)"),
        level = DeprecationLevel.WARNING
    )
    fun userCompletedAction(action: String, metaData: JSONObject?) {
        preservationManager.handleLegacyApiCall(
            methodName = "userCompletedAction",
            parameters = arrayOf(action, metaData)
        )
    }
    
    /**
     * Legacy sendCommerceEvent wrapper.
     */
    @Deprecated(
        message = "Use eventManager.logEvent() with commerce data instead",
        replaceWith = ReplaceWith("ModernBranchCore.getInstance().eventManager.logEvent(eventData)"),
        level = DeprecationLevel.WARNING
    )
    fun sendCommerceEvent(
        revenue: Double,
        currency: String,
        metadata: JSONObject?,
        callback: Branch.BranchReferralStateChangedListener?
    ) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "sendCommerceEvent",
            parameters = arrayOf(revenue, currency, metadata, callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptCommerceCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy loadRewards wrapper.
     */
    @Deprecated(
        message = "Use rewardsManager.loadRewards() instead (if rewards system is still needed)",
        level = DeprecationLevel.WARNING
    )
    fun loadRewards(callback: Branch.BranchReferralStateChangedListener?) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "loadRewards",
            parameters = arrayOf(callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptRewardsCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy getCredits wrapper.
     */
    @Deprecated(
        message = "Use rewardsManager.getCredits() instead (if rewards system is still needed)",
        level = DeprecationLevel.WARNING
    )
    fun getCredits(): Int {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getCredits",
            parameters = emptyArray()
        )
        return result as? Int ?: 0
    }
    
    /**
     * Legacy redeemRewards wrapper.
     */
    @Deprecated(
        message = "Use rewardsManager.redeemRewards() instead (if rewards system is still needed)",
        level = DeprecationLevel.WARNING
    )
    fun redeemRewards(
        count: Int,
        callback: Branch.BranchReferralStateChangedListener?
    ) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "redeemRewards",
            parameters = arrayOf(count, callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptRewardsCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy getCreditHistory wrapper.
     */
    @Deprecated(
        message = "Use rewardsManager.getCreditHistory() instead (if rewards system is still needed)",
        level = DeprecationLevel.WARNING
    )
    fun getCreditHistory(callback: Branch.BranchListResponseListener?) {
        val result = preservationManager.handleLegacyApiCall(
            methodName = "getCreditHistory",
            parameters = arrayOf(callback)
        )
        
        if (callback != null) {
            callbackRegistry.adaptHistoryCallback(callback, result, null)
        }
    }
    
    /**
     * Legacy enableTestMode wrapper.
     */
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
     * Legacy disableTracking wrapper.
     */
    @Deprecated(
        message = "Use configurationManager.setTrackingDisabled() instead",
        level = DeprecationLevel.WARNING
    )
    fun disableTracking(disabled: Boolean) {
        preservationManager.handleLegacyApiCall(
            methodName = "disableTracking",
            parameters = arrayOf(disabled)
        )
    }
    
    /**
     * Get current preservation manager status.
     */
    fun isModernCoreReady(): Boolean {
        return preservationManager.isReady()
    }
    
    /**
     * Get usage analytics for this wrapper.
     */
    fun getUsageAnalytics() = preservationManager.getUsageAnalytics()
    
    /**
     * Override toString to provide clear identification.
     */
    override fun toString(): String {
        return "LegacyBranchWrapper(modernCore=${preservationManager.isReady()})"
    }
} 