package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import org.json.JSONObject
import io.branch.referral.network.BranchRemoteInterface

/**
 * Immutable configuration for the Branch SDK. Build it in [Builder] and pass it to
 * [Branch.initialize], or to [Branch.updateConfiguration] to change settings later. A null
 * setting is one this configuration doesn't set.
 */
class BranchConfiguration private constructor(
    val branchKey: String?,
    val testMode: Boolean,
    val apiUrl: String?,
    val cdnBaseUrl: String?,
    val euEndpoint: Boolean?,
    val logLevel: BranchLogger.BranchLogLevel,
    private val logLevelWasSet: Boolean,
    val loggingCallback: IBranchLoggingCallbacks?,
    val requestTracingCallback: IBranchRequestTracingCallback?,
    val networkTimeout: Int?,
    val networkConnectTimeout: Int?,
    val retryCount: Int?,
    val retryInterval: Int?,
    val noConnectionRetryMax: Int?,
    val remoteInterface: BranchRemoteInterface?,
    val attributionLevel: Defines.BranchAttributionLevel?,
    val dmaParameters: DMAParameters?,
    val limitFacebookAttribution: Boolean?,
    val adNetworkCalloutsDisabled: Boolean?,
    val facebookAppId: String?,
    val preinstallCampaign: String?,
    val preinstallPartner: String?,
    val installMetadata: Map<String, String>,
    val referringLinkAttributionForPreinstalledApps: Boolean?,
    val whitelistedSchemes: List<String>,
    val uriHostsToSkip: List<String>,
    val userAgentFetchSync: Boolean?
) {

    /**
     * Routes logging to the caller's level and callback. Called by [Branch.initialize] before any
     * other init work, so warnings raised during construction and branch-key resolution are visible
     * to whoever configured logging. Leaves the logger alone when neither a level nor a callback was
     * set, so an earlier [Branch.enableLogging] call stays in effect, and keeps an earlier callback
     * when only a level is set.
     */
    @JvmName("applyLogging")
    internal fun applyLogging() {
        if (!logLevelWasSet && loggingCallback == null) return
        loggingCallback?.let { BranchLogger.loggerCallback = it }
        BranchLogger.loggingLevel = logLevel
        BranchLogger.loggingEnabled = true
    }

    /**
     * Writes every configured value through to the subsystem that owns it. Called once by
     * [Branch.initialize].
     */
    @JvmName("applyTo")
    internal fun applyTo(branch: Branch) {
        val prefHelper = branch.prefHelper

        BranchLogger.i(Branch.GOOGLE_VERSION_TAG)
        if (BranchLogger.isLoggable(BranchLogger.BranchLogLevel.DEBUG)) BranchLogger.d(toJson())

        // Set once
        BranchUtil.setTestMode(testMode)
        remoteInterface?.let { branch.setBranchRemoteInterface(it) }

        // Install attribution
        facebookAppId?.let { PrefHelper.setFbAppId(it) }
        preinstallCampaign?.let { prefHelper.setPreinstallCampaign(it) }
        preinstallPartner?.let { prefHelper.setPreinstallPartner(it) }
        installMetadata.forEach { (key, value) -> prefHelper.addInstallMetadata(key, value) }
        referringLinkAttributionForPreinstalledApps?.let {
            Branch.referringLinkAttributionForPreinstalledAppsEnabled = it
        }

        // Defaults first, so a setting removed from the configuration doesn't stay saved
        prefHelper.timeout = PrefHelper.TIMEOUT
        prefHelper.connectTimeout = PrefHelper.CONNECT_TIMEOUT
        prefHelper.retryCount = PrefHelper.MAX_RETRIES
        prefHelper.retryInterval = PrefHelper.INTERVAL_RETRY
        prefHelper.noConnectionRetryMax = PrefHelper.DEFAULT_NO_CONNECTION_RETRY_MAX
        prefHelper.setLimitFacebookTracking(false)
        prefHelper.setAdNetworkCalloutsDisabled(false)
        Branch.userAgentSync = false

        applyUpdate(branch)
    }

    /**
     * Writes the settings this configuration sets, except those [applyTo] sets once. Called by
     * [applyTo] and [Branch.updateConfiguration].
     */
    @JvmName("applyUpdate")
    internal fun applyUpdate(branch: Branch) {
        val context = branch.applicationContext
        val prefHelper = branch.prefHelper

        requestTracingCallback?.let { Branch._iBranchRequestTracingCallback = it }

        // Identity & environment
        apiUrl?.let { PrefHelper.setAPIUrl(it) }
        cdnBaseUrl?.let { PrefHelper.setCDNBaseUrl(it) }
        euEndpoint?.let { PrefHelper.useEUEndpoint(it) }

        // Network — validated in Builder.build()
        networkTimeout?.let { prefHelper.timeout = it }
        networkConnectTimeout?.let { prefHelper.connectTimeout = it }
        retryCount?.let { prefHelper.retryCount = it }
        retryInterval?.let { prefHelper.retryInterval = it }
        noConnectionRetryMax?.let { prefHelper.noConnectionRetryMax = it }

        // Privacy & attribution
        attributionLevel?.let { branch.setConsumerProtectionAttributionLevel(it, null) }
        dmaParameters?.let {
            it.logWarnings()
            prefHelper.setDMAParameters(it.eeaRegion, it.adPersonalizationConsent, it.adUserDataUsageConsent)
        }
        limitFacebookAttribution?.let { prefHelper.setLimitFacebookTracking(it) }
        adNetworkCalloutsDisabled?.let { prefHelper.setAdNetworkCalloutsDisabled(it) }

        // URL collection
        if (whitelistedSchemes.isNotEmpty() || uriHostsToSkip.isNotEmpty()) {
            val analyser = UniversalResourceAnalyser.getInstance(context)
            whitelistedSchemes.filter { it.isNotBlank() }.forEach { analyser.addToAcceptURLFormats(it) }
            uriHostsToSkip.filter { it.isNotBlank() }.forEach { analyser.addToSkipURLFormats(it) }
        }

        // User agent
        userAgentFetchSync?.let { Branch.userAgentSync = it }
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
        fun lit(name: String, value: Any?) {
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
        lit("userAgentFetchSync", userAgentFetchSync)

        return json.append('}').toString()
    }

    /** Branch keys are client-side, but there is no reason to spill a whole one into logcat. */
    private fun maskedKey(): String? =
        branchKey?.let { if (it.length > 13) it.take(9) + "..." + it.takeLast(4) else "***" }

    /** Lists only the settings this configuration sets. */
    override fun toString(): String {
        val nonDefaults = mutableListOf<String>()
        maskedKey()?.let { nonDefaults.add("branchKey=$it") }
        if (testMode) nonDefaults.add("testMode=true")
        apiUrl?.let { nonDefaults.add("apiUrl=$it") }
        cdnBaseUrl?.let { nonDefaults.add("cdnBaseUrl=$it") }
        euEndpoint?.let { nonDefaults.add("euEndpoint=$it") }
        if (logLevel != DEFAULT_LOG_LEVEL) nonDefaults.add("logLevel=$logLevel")
        if (loggingCallback != null) nonDefaults.add("loggingCallback=set")
        if (requestTracingCallback != null) nonDefaults.add("requestTracingCallback=set")
        networkTimeout?.let { nonDefaults.add("networkTimeout=$it") }
        networkConnectTimeout?.let { nonDefaults.add("networkConnectTimeout=$it") }
        retryCount?.let { nonDefaults.add("retryCount=$it") }
        retryInterval?.let { nonDefaults.add("retryInterval=$it") }
        noConnectionRetryMax?.let { nonDefaults.add("noConnectionRetryMax=$it") }
        if (remoteInterface != null) nonDefaults.add("remoteInterface=${remoteInterface.javaClass.name}")
        attributionLevel?.let { nonDefaults.add("attributionLevel=$it") }
        dmaParameters?.let { nonDefaults.add("dmaParameters=$it") }
        limitFacebookAttribution?.let { nonDefaults.add("limitFacebookAttribution=$it") }
        adNetworkCalloutsDisabled?.let { nonDefaults.add("adNetworkCalloutsDisabled=$it") }
        facebookAppId?.let { nonDefaults.add("facebookAppId=$it") }
        preinstallCampaign?.let { nonDefaults.add("preinstallCampaign=$it") }
        preinstallPartner?.let { nonDefaults.add("preinstallPartner=$it") }
        if (installMetadata.isNotEmpty()) nonDefaults.add("installMetadata=${installMetadata.keys}")
        referringLinkAttributionForPreinstalledApps?.let { nonDefaults.add("referringLinkAttributionForPreinstalledApps=$it") }
        if (whitelistedSchemes.isNotEmpty()) nonDefaults.add("whitelistedSchemes=$whitelistedSchemes")
        if (uriHostsToSkip.isNotEmpty()) nonDefaults.add("uriHostsToSkip=$uriHostsToSkip")
        userAgentFetchSync?.let { nonDefaults.add("userAgentFetchSync=$it") }
        return "BranchConfiguration(${nonDefaults.joinToString(", ")})"
    }

    internal companion object {
        internal val DEFAULT_LOG_LEVEL = BranchLogger.BranchLogLevel.NONE

        internal const val MISSING_BRANCH_KEY = "Branch key cannot be empty. Get your key from dashboard.branch.io/settings."

        /** Discriminator for the single-line JSON emitted by [applyTo]. */
        internal const val EVENT_CONFIGURATION_APPLIED = "branch_configuration_applied"
    }

    /** @param branchKey Your Branch key. Required by [Branch.initialize]; leave it out for [Branch.updateConfiguration]. */
    class Builder @JvmOverloads constructor(private val branchKey: String? = null) {
        private var testMode: Boolean = false
        private var apiUrl: String? = null
        private var cdnBaseUrl: String? = null
        private var euEndpoint: Boolean? = null
        private var logLevel: BranchLogger.BranchLogLevel = DEFAULT_LOG_LEVEL
        private var logLevelWasSet: Boolean = false
        private var loggingCallback: IBranchLoggingCallbacks? = null
        private var requestTracingCallback: IBranchRequestTracingCallback? = null
        private var networkTimeout: Int? = null
        private var networkConnectTimeout: Int? = null
        private var retryCount: Int? = null
        private var retryInterval: Int? = null
        private var noConnectionRetryMax: Int? = null
        private var remoteInterface: BranchRemoteInterface? = null
        private var attributionLevel: Defines.BranchAttributionLevel? = null
        private var dmaParameters: DMAParameters? = null
        private var limitFacebookAttribution: Boolean? = null
        private var adNetworkCalloutsDisabled: Boolean? = null
        private var facebookAppId: String? = null
        private var preinstallCampaign: String? = null
        private var preinstallPartner: String? = null
        private val installMetadata: MutableMap<String, String> = mutableMapOf()
        private var referringLinkAttributionForPreinstalledApps: Boolean? = null
        private val whitelistedSchemes: MutableList<String> = mutableListOf()
        private val uriHostsToSkip: MutableList<String> = mutableListOf()
        private var userAgentFetchSync: Boolean? = null

        // Identity & environment
        fun setTestMode(enabled: Boolean) = apply { testMode = enabled }
        fun setApiUrl(url: String) = apply { apiUrl = url }
        fun setCdnBaseUrl(url: String) = apply { cdnBaseUrl = url }
        fun setEUEndpoint(enabled: Boolean) = apply { euEndpoint = enabled }

        // Logging
        fun setLogLevel(level: BranchLogger.BranchLogLevel) = apply { logLevel = level; logLevelWasSet = true }
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
        fun setUserAgentFetchSync(sync: Boolean) = apply { userAgentFetchSync = sync }

        /**
         * @throws IllegalArgumentException listing every field that failed validation, so a caller
         * with several bad values fixes them in one pass rather than one per run.
         */
        fun build(): BranchConfiguration {
            val errors = mutableListOf<String>()
            if (branchKey != null && branchKey.isBlank()) {
                errors += MISSING_BRANCH_KEY
            }
            if (networkTimeout?.let { it <= 0 } == true) {
                errors += "Network timeout must be a positive number of milliseconds (got $networkTimeout)."
            }
            if (networkConnectTimeout?.let { it <= 0 } == true) {
                errors += "Network connect timeout must be a positive number of milliseconds (got $networkConnectTimeout)."
            }
            if (retryCount?.let { it < 0 } == true) {
                errors += "Retry count must be >= 0 (got $retryCount)."
            }
            if (retryInterval?.let { it <= 0 } == true) {
                errors += "Retry interval must be a positive number of milliseconds (got $retryInterval)."
            }
            if (noConnectionRetryMax?.let { it <= 0 } == true) {
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
                logLevel = if (!logLevelWasSet && loggingCallback != null) BranchLogger.BranchLogLevel.VERBOSE else logLevel,
                logLevelWasSet = logLevelWasSet,
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
                userAgentFetchSync = userAgentFetchSync
            )
        }
    }
}
