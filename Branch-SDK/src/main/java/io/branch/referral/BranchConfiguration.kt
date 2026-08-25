package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import io.branch.referral.network.BranchRemoteInterface

/**
 * Immutable pre-init configuration for the Branch SDK. Build once in [Builder], pass to
 * [Branch.initialize]. All fields are locked after [Builder.build] is called.
 */
class BranchConfiguration private constructor(
    val branchKey: String,
    val testMode: Boolean,
    val apiUrl: String?,
    val cdnBaseUrl: String?,
    val euEndpoint: Boolean,
    val logLevel: BranchLogger.BranchLogLevel,
    val loggingCallback: IBranchLoggingCallbacks?,
    val requestTracingCallback: IBranchRequestTracingCallback?,
    val networkTimeout: Int,
    val networkConnectTimeout: Int,
    val retryCount: Int,
    val retryInterval: Int,
    val noConnectionRetryMax: Int,
    val remoteInterface: BranchRemoteInterface?,
    val attributionLevel: Defines.BranchAttributionLevel?,
    val dmaParameters: DMAParameters?,
    val limitFacebookAttribution: Boolean,
    val adNetworkCalloutsDisabled: Boolean,
    val facebookAppId: String?,
    val preinstallCampaign: String?,
    val preinstallPartner: String?,
    val installMetadata: Map<String, String>,
    val referringLinkAttributionForPreinstalledApps: Boolean,
    val whitelistedSchemes: List<String>,
    val uriHostsToSkip: List<String>,
    val automaticOpenEvents: Boolean,
    val userAgentFetchSync: Boolean
) {

    /**
     * Writes every configured value through to the subsystem that owns it. Called once by
     * [Branch.initialize]; this is the only place pre-init state is applied.
     */
    @JvmName("applyTo")
    internal fun applyTo(branch: Branch) {
        val context = branch.applicationContext
        val prefHelper = branch.prefHelper

        // Logging — logLevel always has a value, so initialize() owns the logger state entirely.
        BranchLogger.loggerCallback = loggingCallback
        BranchLogger.loggingLevel = logLevel
        BranchLogger.loggingEnabled = true
        BranchLogger.logAlways(Branch.GOOGLE_VERSION_TAG)
        requestTracingCallback?.let { Branch._iBranchRequestTracingCallback = it }

        // Identity & environment
        BranchUtil.setTestMode(testMode)
        apiUrl?.let { PrefHelper.setAPIUrl(it) }
        cdnBaseUrl?.let { PrefHelper.setCDNBaseUrl(it) }
        if (euEndpoint) PrefHelper.useEUEndpoint(true)

        // Network
        if (networkTimeout > 0) prefHelper.timeout = networkTimeout
        if (networkConnectTimeout > 0) prefHelper.connectTimeout = networkConnectTimeout
        if (retryCount >= 0) prefHelper.retryCount = retryCount
        if (retryInterval > 0) prefHelper.retryInterval = retryInterval
        if (noConnectionRetryMax > 0) prefHelper.noConnectionRetryMax = noConnectionRetryMax
        remoteInterface?.let { branch.setBranchRemoteInterface(it) }

        // Privacy & attribution
        attributionLevel?.let { branch.setConsumerProtectionAttributionLevel(it) }
        dmaParameters?.let {
            prefHelper.setEEARegion(it.eeaRegion)
            prefHelper.setAdPersonalizationConsent(it.adPersonalizationConsent)
            prefHelper.setAdUserDataUsageConsent(it.adUserDataUsageConsent)
        }
        prefHelper.setLimitFacebookTracking(limitFacebookAttribution)
        prefHelper.setAdNetworkCalloutsDisabled(adNetworkCalloutsDisabled)

        // Install attribution
        facebookAppId?.let { PrefHelper.setFbAppId(it) }
        preinstallCampaign?.let { prefHelper.setPreinstallCampaign(it) }
        preinstallPartner?.let { prefHelper.setPreinstallPartner(it) }
        installMetadata.forEach { (key, value) -> prefHelper.addInstallMetadata(key, value) }
        if (referringLinkAttributionForPreinstalledApps) {
            Branch.referringLinkAttributionForPreinstalledAppsEnabled = true
        }

        // URL collection
        if (whitelistedSchemes.isNotEmpty() || uriHostsToSkip.isNotEmpty()) {
            val analyser = UniversalResourceAnalyser.getInstance(context)
            whitelistedSchemes.filter { it.isNotBlank() }.forEach { analyser.addToAcceptURLFormats(it) }
            uriHostsToSkip.filter { it.isNotBlank() }.forEach { analyser.addToSkipURLFormats(it) }
        }

        // User agent
        Branch.userAgentSync = userAgentFetchSync
    }

    class Builder(private val branchKey: String) {
        private var testMode: Boolean = false
        private var apiUrl: String? = null
        private var cdnBaseUrl: String? = null
        private var euEndpoint: Boolean = false
        private var logLevel: BranchLogger.BranchLogLevel = BranchLogger.BranchLogLevel.ERROR
        private var loggingCallback: IBranchLoggingCallbacks? = null
        private var requestTracingCallback: IBranchRequestTracingCallback? = null
        private var networkTimeout: Int = PrefHelper.TIMEOUT
        private var networkConnectTimeout: Int = PrefHelper.CONNECT_TIMEOUT
        private var retryCount: Int = PrefHelper.MAX_RETRIES
        private var retryInterval: Int = 1000 // mirrors PrefHelper.INTERVAL_RETRY
        private var noConnectionRetryMax: Int = PrefHelper.DEFAULT_NO_CONNECTION_RETRY_MAX
        private var remoteInterface: BranchRemoteInterface? = null
        private var attributionLevel: Defines.BranchAttributionLevel? = null
        private var dmaParameters: DMAParameters? = null
        private var limitFacebookAttribution: Boolean = false
        private var adNetworkCalloutsDisabled: Boolean = false
        private var facebookAppId: String? = null
        private var preinstallCampaign: String? = null
        private var preinstallPartner: String? = null
        private val installMetadata: MutableMap<String, String> = mutableMapOf()
        private var referringLinkAttributionForPreinstalledApps: Boolean = false
        private val whitelistedSchemes: MutableList<String> = mutableListOf()
        private val uriHostsToSkip: MutableList<String> = mutableListOf()
        private var automaticOpenEvents: Boolean = true
        private var userAgentFetchSync: Boolean = false

        // Identity & environment
        fun setTestMode(enabled: Boolean) = apply { testMode = enabled }
        fun setApiUrl(url: String) = apply { apiUrl = url }
        fun setCdnBaseUrl(url: String) = apply { cdnBaseUrl = url }
        fun setEUEndpoint(enabled: Boolean) = apply { euEndpoint = enabled }

        // Logging
        fun setLogLevel(level: BranchLogger.BranchLogLevel) = apply { logLevel = level }
        fun setLoggingCallback(callback: IBranchLoggingCallbacks?) = apply { loggingCallback = callback }
        fun setRequestTracingCallback(callback: IBranchRequestTracingCallback?) = apply { requestTracingCallback = callback }

        // Network
        fun setNetworkTimeout(timeoutMs: Int) = apply { networkTimeout = timeoutMs }
        fun setNetworkConnectTimeout(timeoutMs: Int) = apply { networkConnectTimeout = timeoutMs }
        fun setRetryCount(count: Int) = apply { retryCount = count }
        fun setRetryInterval(intervalMs: Int) = apply { retryInterval = intervalMs }
        fun setNoConnectionRetryMax(max: Int) = apply { noConnectionRetryMax = max }
        fun setRemoteInterface(remoteInterface: BranchRemoteInterface?) = apply { this.remoteInterface = remoteInterface }

        // Privacy & attribution
        fun setAttributionLevel(level: Defines.BranchAttributionLevel) = apply { attributionLevel = level }
        fun setDMAParameters(params: DMAParameters) = apply { dmaParameters = params }
        fun setLimitFacebookAttribution(limit: Boolean) = apply { limitFacebookAttribution = limit }
        fun setAdNetworkCalloutsDisabled(disabled: Boolean) = apply { adNetworkCalloutsDisabled = disabled }

        // Install attribution
        fun setFacebookAppId(appId: String) = apply { facebookAppId = appId }
        fun setPreinstallCampaign(campaign: String) = apply { preinstallCampaign = campaign }
        fun setPreinstallPartner(partner: String) = apply { preinstallPartner = partner }
        fun addInstallMetadata(key: String, value: String) = apply { installMetadata[key] = value }
        fun setReferringLinkAttributionForPreinstalledApps(enabled: Boolean) = apply {
            referringLinkAttributionForPreinstalledApps = enabled
        }

        // URL collection
        fun addWhitelistedScheme(scheme: String) = apply { whitelistedSchemes.add(scheme) }
        fun addUriHostToSkip(host: String) = apply { uriHostsToSkip.add(host) }

        // Open tracking
        /** When false, [ProcessLifecycleOwner] won't call [Branch.sendOpen] automatically on ON_START. */
        fun setAutomaticOpenEvents(enabled: Boolean) = apply { automaticOpenEvents = enabled }
        fun setUserAgentFetchSync(sync: Boolean) = apply { userAgentFetchSync = sync }

        /** @throws IllegalArgumentException if any field fails validation. */
        fun build(): BranchConfiguration {
            require(branchKey.isNotBlank()) {
                "Branch key cannot be empty. Get your key from dashboard.branch.io/settings."
            }
            require(networkTimeout > 0) {
                "Network timeout must be a positive number of milliseconds (got $networkTimeout)."
            }
            require(networkTimeout <= 60_000) {
                "Network timeout cannot exceed 60 seconds / 60000 ms (got $networkTimeout)."
            }
            require(networkConnectTimeout > 0) {
                "Network connect timeout must be a positive number of milliseconds (got $networkConnectTimeout)."
            }
            require(networkConnectTimeout <= 60_000) {
                "Network connect timeout cannot exceed 60 seconds / 60000 ms (got $networkConnectTimeout)."
            }
            require(retryCount >= 0) {
                "Retry count must be >= 0 (got $retryCount)."
            }

            return BranchConfiguration(
                branchKey = branchKey,
                testMode = testMode,
                apiUrl = apiUrl,
                cdnBaseUrl = cdnBaseUrl,
                euEndpoint = euEndpoint,
                logLevel = logLevel,
                loggingCallback = loggingCallback,
                requestTracingCallback = requestTracingCallback,
                networkTimeout = networkTimeout,
                networkConnectTimeout = networkConnectTimeout,
                retryCount = retryCount,
                retryInterval = retryInterval,
                noConnectionRetryMax = noConnectionRetryMax,
                remoteInterface = remoteInterface,
                attributionLevel = attributionLevel,
                dmaParameters = dmaParameters,
                limitFacebookAttribution = limitFacebookAttribution,
                adNetworkCalloutsDisabled = adNetworkCalloutsDisabled,
                facebookAppId = facebookAppId,
                preinstallCampaign = preinstallCampaign,
                preinstallPartner = preinstallPartner,
                installMetadata = installMetadata.toMap(),
                referringLinkAttributionForPreinstalledApps = referringLinkAttributionForPreinstalledApps,
                whitelistedSchemes = whitelistedSchemes.toList(),
                uriHostsToSkip = uriHostsToSkip.toList(),
                automaticOpenEvents = automaticOpenEvents,
                userAgentFetchSync = userAgentFetchSync
            )
        }
    }
}
