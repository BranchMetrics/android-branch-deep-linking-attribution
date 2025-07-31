package io.branch.referral

import org.json.JSONException
import org.json.JSONObject

/**
 * Controller class for managing Branch SDK configurations.
 * This class provides methods to control and track various configuration settings of the Branch SDK.
 * It is used internally by the Branch SDK to manage configuration states.
 */
class BranchConfigurationController {
    
    companion object {
        const val KEY_INSTANT_DEEP_LINKING_ENABLED = "bnc_instant_deep_linking_enabled"
        const val KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME = "bnc_defer_init_for_plugin_runtime"
    }

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
     * @return Boolean indicating if delayed session initialization was used
     * @see Branch.expectDelayedSessionInitialization
     */
    private fun getDelayedSessionInitUsed(): Boolean {
        return Branch.getInstance()?.prefHelper_?.delayedSessionInitUsed ?: false
    }

    /**
     * Gets whether test mode is enabled.
     * @return Boolean indicating if test mode is enabled
     */
    fun isTestModeEnabled(): Boolean {
        return BranchUtil.isTestModeEnabled()
    }

    /**
     * Sets whether test mode should be enabled.
     * When test mode is enabled, the SDK will use test keys and endpoints.
     * This is useful for development and testing purposes.
     * @param enabled Boolean indicating if test mode should be enabled
     */
    fun setTestModeEnabled(enabled: Boolean) {
        BranchUtil.setTestMode(enabled)
    }

    /**
     * Sets whether instant deep linking is enabled.
     * This flag controls whether the SDK should attempt to perform instant deep linking.
     * @param enabled Boolean indicating if instant deep linking should be enabled
     */
    fun setInstantDeepLinkingEnabled(enabled: Boolean) {
        Branch.getInstance()?.prefHelper_?.setBool(KEY_INSTANT_DEEP_LINKING_ENABLED, enabled)
    }

    /**
     * Gets whether instant deep linking is enabled.
     * @return Boolean indicating if instant deep linking is enabled
     */
    fun isInstantDeepLinkingEnabled(): Boolean {
        return Branch.getInstance()?.prefHelper_?.getBool(KEY_INSTANT_DEEP_LINKING_ENABLED) ?: false
    }

    /**
     * Sets whether plugin runtime initialization should be deferred.
     * This is used for cross-process communication scenarios like React Native,
     * where we need to wait for the plugin to signal when it's ready before initializing.
     * @param deferred Boolean indicating if plugin runtime initialization should be deferred
     */
    fun setDeferInitForPluginRuntime(deferred: Boolean) {
        Branch.getInstance()?.prefHelper_?.setBool(KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME, deferred)
    }

    /**
     * Gets whether plugin runtime initialization is deferred.
     * @return Boolean indicating if plugin runtime initialization is deferred
     */
    private fun isDeferInitForPluginRuntime(): Boolean {
        return Branch.getInstance()?.prefHelper_?.getBool(KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME) ?: false
    }

    /**
     * Gets the source of the Branch key configuration.
     * This indicates where the Branch key was configured from (e.g., branch.json, manifest, strings.xml, constructor, public_function).
     * 
     * @return String indicating the source of the Branch key, or "unknown" if not set
     */
    fun getBranchKeySource(): String {
        return Branch.getInstance()?.prefHelper_?.branchKeySource ?: "unknown"
    }

    /**
     * Checks if the Branch key configuration involved a fallback from test key to live key.
     * This happens when test mode is enabled but no test key is configured in the manifest.
     * 
     * @return Boolean indicating if a fallback from test key to live key occurred
     */
    fun isBranchKeyFallbackUsed(): Boolean {
        return getBranchKeySource() == "branchKey"
    }

    /**
     * Serializes the current configuration state into a JSONObject.
     * This is used to send configuration data to the server.
     * @return JSONObject containing the current configuration state
     */
    fun serializeConfiguration(): JSONObject {
        return try {
            JSONObject().apply {
                put("expectDelayedSessionInitialization", getDelayedSessionInitUsed())
                put("testMode", isTestModeEnabled())
                put("instantDeepLinkingEnabled", isInstantDeepLinkingEnabled())
                put("deferInitForPluginRuntime", isDeferInitForPluginRuntime())
                put("branch_key_source", getBranchKeySource())
                put("branch_key_fallback_used", isBranchKeyFallbackUsed())
            }
        } catch (e: NullPointerException) {
            BranchLogger.w("Error serializing configuration - null reference: ${e.message}")
            JSONObject()
        } catch (e: JSONException) {
            BranchLogger.w("Error serializing configuration - JSON error: ${e.message}")
            JSONObject()
        } catch (e: Exception) {
            BranchLogger.w("Error serializing configuration - unexpected error: ${e.message}")
            JSONObject()
        }
    }
} 