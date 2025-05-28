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
        Branch.getInstance().prefHelper_.delayedSessionInitUsed = used
    }

    /**
     * Gets whether delayed session initialization was used.
     * This can be used to check if the app has previously used delayed session initialization.
     * 
     * @return Boolean indicating if delayed session initialization was used
     * @see Branch.expectDelayedSessionInitialization
     */
    private fun getDelayedSessionInitUsed(): Boolean {
        return Branch.getInstance().prefHelper_.delayedSessionInitUsed
    }

    /**
     * Sets whether test mode is enabled.
     * This flag is used to track if the app is running in test mode.
     */
    fun setTestModeEnabled(enabled: Boolean) {
        if (enabled) {
            Branch.enableTestMode()
        } else {
            Branch.disableTestMode()
        }
    }

    /**
     * Gets whether test mode is enabled.
     * 
     * @return Boolean indicating if test mode is enabled
     */
    private fun isTestModeEnabled(): Boolean {
        return Branch.getInstance().prefHelper_.getBool("bnc_test_mode")
    }

    /**
     * Sets whether tracking is disabled.
     * This flag is used to track if the app has disabled tracking.
     */
    fun setTrackingDisabled(disabled: Boolean) {
        Branch.getInstance().prefHelper_.setBool("bnc_tracking_disabled", disabled)
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
     * Sets whether instant deep linking is enabled.
     * This flag is used to track if the app has enabled instant deep linking.
     */
    fun setInstantDeepLinkingEnabled(enabled: Boolean) {
        Branch.getInstance().prefHelper_.setBool("bnc_instant_deep_linking_enabled", enabled)
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
     * Serializes the current configuration state into a JSONObject.
     * This is used to send configuration data to the server.
     * 
     * @return JSONObject containing the current configuration state
     */
    fun serializeConfiguration(): JSONObject {
        return JSONObject().apply {
            put("expectDelayedSessionInitialization", getDelayedSessionInitUsed())
            put("testMode", isTestModeEnabled())
            put("trackingDisabled", isTrackingDisabled())
            put("instantDeepLinkingEnabled", isInstantDeepLinkingEnabled())
        }
    }
} 