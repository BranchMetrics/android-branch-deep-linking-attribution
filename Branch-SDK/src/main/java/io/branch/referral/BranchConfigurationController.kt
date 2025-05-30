package io.branch.referral

import org.json.JSONObject

/**
 * Controller class for managing Branch SDK configurations.
 * This class provides methods to control and track various configuration settings of the Branch SDK.
 * It is used internally by the Branch SDK to manage configuration states.
 */
class BranchConfigurationController {
    /**
     * Sets whether delayed session initialization was used.
     * This flag is used to track if the app has used delayed session initialization,
     * which is important for analytics and debugging purposes.
     * 
     * @param used Boolean indicating if delayed session initialization was used
     * @see Branch.expectDelayedSessionInitialization
     */
    fun setDelayedSessionInitUsed(used: Boolean) {
        Branch.getInstance()?.let { branch ->
            branch.prefHelper_.delayedSessionInitUsed = used
        }
    }

    /**
     * Gets whether delayed session initialization was used.
     * This can be used to check if the app has previously used delayed session initialization.
     * 
     * @return Boolean indicating if delayed session initialization was used
     * @see Branch.expectDelayedSessionInitialization
     */
    private fun getDelayedSessionInitUsed(): Boolean {
        return Branch.getInstance()?.prefHelper_?.delayedSessionInitUsed ?: false
    }

    /**
     * Gets whether test mode is enabled.
     * 
     * @return Boolean indicating if test mode is enabled
     */
    private fun isTestModeEnabled(): Boolean {
        return BranchUtil.isTestModeEnabled()
    }

    /**
     * Gets whether tracking is disabled.
     * 
     * @return Boolean indicating if tracking is disabled
     */
    fun isTrackingDisabled(): Boolean {
        return Branch.getInstance().prefHelper_.getBool("bnc_tracking_disabled")
    }

    /**
     * Gets whether instant deep linking is enabled.
     * 
     * @return Boolean indicating if instant deep linking is enabled
     */
    private fun isInstantDeepLinkingEnabled(): Boolean {
        return Branch.getInstance().prefHelper_.getBool("bnc_instant_deep_linking_enabled")
    }

    /**
     * Sets whether plugin runtime initialization should be deferred.
     * This is used for cross-process communication scenarios like React Native,
     * where we need to wait for the plugin to signal when it's ready before initializing.
     * 
     * @param deferred Boolean indicating if plugin runtime initialization should be deferred
     */
    fun setDeferInitForPluginRuntime(deferred: Boolean) {
        Branch.getInstance()?.prefHelper_?.setBool("bnc_defer_init_for_plugin_runtime", deferred)
    }

    /**
     * Gets whether plugin runtime initialization is deferred.
     * 
     * @return Boolean indicating if plugin runtime initialization is deferred
     */
    private fun isDeferInitForPluginRuntime(): Boolean {
        return Branch.getInstance().prefHelper_.getBool("bnc_defer_init_for_plugin_runtime")
    }

    /**
     * Serializes the current configuration state into a JSONObject.
     * This is used to send configuration data to the server.
     * 
     * @return JSONObject containing the current configuration state
     */
    fun serializeConfiguration(): JSONObject {
        return try {
            JSONObject().apply {
                put("expectDelayedSessionInitialization", getDelayedSessionInitUsed())
                put("testMode", isTestModeEnabled())
                put("trackingDisabled", isTrackingDisabled())
                put("instantDeepLinkingEnabled", isInstantDeepLinkingEnabled())
                put("deferInitForPluginRuntime", isDeferInitForPluginRuntime())
            }
        } catch (e: Exception) {
            BranchLogger.w("Error serializing configuration: ${e.message}")
            JSONObject()
        }
    }
} 