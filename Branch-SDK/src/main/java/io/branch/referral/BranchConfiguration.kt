package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import org.json.JSONObject
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
     * Routes logging to the caller's level and callback. Called by [Branch.initialize] before any
     * other init work, so warnings raised during construction and branch-key resolution are visible
     * to whoever configured logging.
     */
    @JvmName("applyLogging")
    internal fun applyLogging() {
        BranchLogger.loggerCallback = loggingCallback
        BranchLogger.loggingLevel = logLevel
        BranchLogger.loggingEnabled = true
    }

    /**
     * Writes every configured value through to the subsystem that owns it. Called once by
     * [Branch.initialize].
     */
    @JvmName("applyTo")
    internal fun applyTo(branch: Branch) {
        val context = branch.applicationContext
        val prefHelper = branch.prefHelper

        BranchLogger.logAlways(Branch.GOOGLE_VERSION_TAG)
        if (BranchLogger.isLoggable(BranchLogger.BranchLogLevel.DEBUG)) BranchLogger.d(toJson())
        requestTracingCallback?.let { Branch._iBranchRequestTracingCallback = it }

        // Identity & environment
        BranchUtil.setTestMode(testMode)
        apiUrl?.let { PrefHelper.setAPIUrl(it) }
        cdnBaseUrl?.let { PrefHelper.setCDNBaseUrl(it) }
        if (euEndpoint) PrefHelper.useEUEndpoint(true)

        // Network — validated in Builder.build()
        prefHelper.timeout = networkTimeout
        prefHelper.connectTimeout = networkConnectTimeout
        prefHelper.retryCount = retryCount
        prefHelper.retryInterval = retryInterval
        prefHelper.noConnectionRetryMax = noConnectionRetryMax
        remoteInterface?.let { branch.setBranchRemoteInterface(it) }

        // Privacy & attribution
        attributionLevel?.let { branch.setConsumerProtectionAttributionLevel(it, null) }
        dmaParameters?.let {
            it.logWarnings()
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

    /**
     * The full configuration as a single-line JSON object, so log output can be parsed and asserted
     * on rather than scraped across several lines. Field order is fixed, and every field is always
     * present, so both `JSONObject(line)` and exact-string assertions work.
     */
    internal fun toJson(): String {
        val json = StringBuilder("{")

        fun key(name: String) {
            if (json.length > 1) json.append(',')
            json.append(JSONObject.quote(name)).append(':')
        }
        fun str(name: String, value: String?) {
            key(name)
            json.append(if (value == null) "null" else JSONObject.quote(value))
        }
        fun lit(name: String, value: Any) {
            key(name)
            json.append(value)
        }
        fun raw(name: String, encoded: String) {
            key(name)
            json.append(encoded)
        }

        str("event", EVENT_CONFIGURATION_APPLIED)
        str("branchKey", maskedKey())
        lit("testMode", testMode)
        str("apiUrl", apiUrl)
        str("cdnBaseUrl", cdnBaseUrl)
        lit("euEndpoint", euEndpoint)
        str("logLevel", logLevel.name)
        str("loggingCallback", loggingCallback?.javaClass?.name)
        str("requestTracingCallback", requestTracingCallback?.javaClass?.name)
        lit("networkTimeout", networkTimeout)
        lit("networkConnectTimeout", networkConnectTimeout)
        lit("retryCount", retryCount)
        lit("retryInterval", retryInterval)
        lit("noConnectionRetryMax", noConnectionRetryMax)
        str("remoteInterface", remoteInterface?.javaClass?.name)
        str("attributionLevel", attributionLevel?.name)
        raw("dmaParameters", dmaParameters?.let {
            "{" + JSONObject.quote("eeaRegion") + ":" + it.eeaRegion +
                    "," + JSONObject.quote("adPersonalizationConsent") + ":" + it.adPersonalizationConsent +
                    "," + JSONObject.quote("adUserDataUsageConsent") + ":" + it.adUserDataUsageConsent + "}"
        } ?: "null")
        lit("limitFacebookAttribution", limitFacebookAttribution)
        lit("adNetworkCalloutsDisabled", adNetworkCalloutsDisabled)
        str("facebookAppId", facebookAppId)
        str("preinstallCampaign", preinstallCampaign)
        str("preinstallPartner", preinstallPartner)
        raw("installMetadata", installMetadata.entries.joinToString(",", "{", "}") {
            JSONObject.quote(it.key) + ":" + JSONObject.quote(it.value)
        })
        lit("referringLinkAttributionForPreinstalledApps", referringLinkAttributionForPreinstalledApps)
        raw("whitelistedSchemes", whitelistedSchemes.joinToString(",", "[", "]") { JSONObject.quote(it) })
        raw("uriHostsToSkip", uriHostsToSkip.joinToString(",", "[", "]") { JSONObject.quote(it) })
        lit("automaticOpenEvents", automaticOpenEvents)
        lit("userAgentFetchSync", userAgentFetchSync)

        return json.append('}').toString()
    }

    /** Branch keys are client-side, but there is no reason to spill a whole one into logcat. */
    private fun maskedKey(): String =
        if (branchKey.length > 13) branchKey.take(9) + "..." + branchKey.takeLast(4) else "***"

    /**
     * Lists only the settings that differ from their defaults, so the DEBUG-level line in
     * [applyTo] reads as "here is what this app actually asked for".
     */
    override fun toString(): String {
        val nonDefaults = mutableListOf("branchKey=${maskedKey()}")
        if (testMode) nonDefaults.add("testMode=true")
        apiUrl?.let { nonDefaults.add("apiUrl=$it") }
        cdnBaseUrl?.let { nonDefaults.add("cdnBaseUrl=$it") }
        if (euEndpoint) nonDefaults.add("euEndpoint=true")
        if (logLevel != DEFAULT_LOG_LEVEL) nonDefaults.add("logLevel=$logLevel")
        if (loggingCallback != null) nonDefaults.add("loggingCallback=set")
        if (requestTracingCallback != null) nonDefaults.add("requestTracingCallback=set")
        if (networkTimeout != PrefHelper.TIMEOUT) nonDefaults.add("networkTimeout=$networkTimeout")
        if (networkConnectTimeout != PrefHelper.CONNECT_TIMEOUT) nonDefaults.add("networkConnectTimeout=$networkConnectTimeout")
        if (retryCount != PrefHelper.MAX_RETRIES) nonDefaults.add("retryCount=$retryCount")
        if (retryInterval != PrefHelper.INTERVAL_RETRY) nonDefaults.add("retryInterval=$retryInterval")
        if (noConnectionRetryMax != PrefHelper.DEFAULT_NO_CONNECTION_RETRY_MAX) nonDefaults.add("noConnectionRetryMax=$noConnectionRetryMax")
        if (remoteInterface != null) nonDefaults.add("remoteInterface=${remoteInterface.javaClass.name}")
        attributionLevel?.let { nonDefaults.add("attributionLevel=$it") }
        dmaParameters?.let { nonDefaults.add("dmaParameters=$it") }
        if (limitFacebookAttribution) nonDefaults.add("limitFacebookAttribution=true")
        if (adNetworkCalloutsDisabled) nonDefaults.add("adNetworkCalloutsDisabled=true")
        facebookAppId?.let { nonDefaults.add("facebookAppId=$it") }
        preinstallCampaign?.let { nonDefaults.add("preinstallCampaign=$it") }
        preinstallPartner?.let { nonDefaults.add("preinstallPartner=$it") }
        if (installMetadata.isNotEmpty()) nonDefaults.add("installMetadata=${installMetadata.keys}")
        if (referringLinkAttributionForPreinstalledApps) nonDefaults.add("referringLinkAttributionForPreinstalledApps=true")
        if (whitelistedSchemes.isNotEmpty()) nonDefaults.add("whitelistedSchemes=$whitelistedSchemes")
        if (uriHostsToSkip.isNotEmpty()) nonDefaults.add("uriHostsToSkip=$uriHostsToSkip")
        if (!automaticOpenEvents) nonDefaults.add("automaticOpenEvents=false")
        if (userAgentFetchSync) nonDefaults.add("userAgentFetchSync=true")
        return "BranchConfiguration(${nonDefaults.joinToString(", ")})"
    }

    internal companion object {
        internal val DEFAULT_LOG_LEVEL = BranchLogger.BranchLogLevel.NONE

        /** Discriminator for the single-line JSON emitted by [applyTo]. */
        internal const val EVENT_CONFIGURATION_APPLIED = "branch_configuration_applied"
    }

    class Builder(private val branchKey: String) {
        private var testMode: Boolean = false
        private var apiUrl: String? = null
        private var cdnBaseUrl: String? = null
        private var euEndpoint: Boolean = false
        private var logLevel: BranchLogger.BranchLogLevel = DEFAULT_LOG_LEVEL
        private var loggingCallback: IBranchLoggingCallbacks? = null
        private var requestTracingCallback: IBranchRequestTracingCallback? = null
        private var networkTimeout: Int = PrefHelper.TIMEOUT
        private var networkConnectTimeout: Int = PrefHelper.CONNECT_TIMEOUT
        private var retryCount: Int = PrefHelper.MAX_RETRIES
        private var retryInterval: Int = PrefHelper.INTERVAL_RETRY
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

        /**
         * @throws IllegalArgumentException listing every field that failed validation, so a caller
         * with several bad values fixes them in one pass rather than one per run.
         */
        fun build(): BranchConfiguration {
            val errors = mutableListOf<String>()
            if (branchKey.isBlank()) {
                errors += "Branch key cannot be empty. Get your key from dashboard.branch.io/settings."
            }
            if (networkTimeout <= 0) {
                errors += "Network timeout must be a positive number of milliseconds (got $networkTimeout)."
            }
            if (networkConnectTimeout <= 0) {
                errors += "Network connect timeout must be a positive number of milliseconds (got $networkConnectTimeout)."
            }
            if (retryCount < 0) {
                errors += "Retry count must be >= 0 (got $retryCount)."
            }
            if (retryInterval <= 0) {
                errors += "Retry interval must be a positive number of milliseconds (got $retryInterval)."
            }
            if (noConnectionRetryMax <= 0) {
                errors += "No-connection retry max must be > 0 (got $noConnectionRetryMax)."
            }
            require(errors.isEmpty()) {
                "Invalid BranchConfiguration:\n  - " + errors.joinToString("\n  - ")
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
