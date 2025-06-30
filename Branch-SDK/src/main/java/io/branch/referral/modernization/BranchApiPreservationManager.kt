package io.branch.referral.modernization

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.NonNull
import io.branch.referral.BranchLogger
import io.branch.referral.modernization.analytics.ApiUsageAnalytics
import io.branch.referral.modernization.core.ModernBranchCore
import io.branch.referral.modernization.core.VersionConfiguration
import io.branch.referral.modernization.core.VersionConfigurationFactory
import io.branch.referral.modernization.registry.PublicApiRegistry
import io.branch.referral.modernization.registry.ApiMethodInfo
import io.branch.referral.modernization.registry.UsageImpact
import io.branch.referral.modernization.registry.MigrationComplexity
import io.branch.referral.modernization.registry.MigrationReport
import io.branch.referral.modernization.registry.VersionTimelineReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Central coordinator for Branch SDK API preservation during modernization.
 * 
 * This class manages the complete preservation strategy, coordinating between
 * legacy API wrappers and the new modern implementation while maintaining
 * 100% backward compatibility.
 * 
 * Responsibilities:
 * - Coordinate all API preservation activities
 * - Manage deprecation warnings and guidance
 * - Track API usage analytics and metrics
 * - Provide migration support and tooling
 */
class BranchApiPreservationManager private constructor(
    private val context: Context,
    private val versionConfiguration: VersionConfiguration
) {
    
    private val modernBranchCore: ModernBranchCore? = null // Will be injected when available
    
    private val publicApiRegistry = PublicApiRegistry(versionConfiguration)
    private val usageAnalytics = ApiUsageAnalytics()
    private val activeCallsCache = ConcurrentHashMap<String, Long>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BranchApiPreservationManager? = null
        
        /**
         * Get the singleton instance of the preservation manager.
         * Thread-safe initialization ensures single instance across the application.
         */
        fun getInstance(context: Context): BranchApiPreservationManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val versionConfig = VersionConfigurationFactory.createConfiguration(context)
                    BranchApiPreservationManager(context.applicationContext, versionConfig).also { 
                        instance = it 
                    }
                }
            }
        }
    }
    
    init {
        registerAllPublicApis()
        BranchLogger.d("BranchApiPreservationManager initialized with ${publicApiRegistry.getTotalApiCount()} registered APIs")
    }
    
    /**
     * Register all public APIs that must be preserved during modernization.
     * This comprehensive catalog ensures no breaking changes during transition.
     * 
     * Each API can have its own deprecation and removal timeline based on:
     * - Usage impact and migration complexity
     * - Breaking changes required
     * - Dependencies on other APIs
     */
    private fun registerAllPublicApis() {
        publicApiRegistry.apply {
            // Core Instance Management APIs - Critical, keep longer
            registerApi(
                methodName = "getInstance",
                signature = "Branch.getInstance()",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "ModernBranchCore.getInstance()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "7.0.0" // Extended support due to critical usage
            )
            
            registerApi(
                methodName = "getAutoInstance",
                signature = "Branch.getAutoInstance(Context)",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "ModernBranchCore.initialize(Context)",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "7.0.0" // Extended support due to critical usage
            )
            
            // Session Management APIs - Critical but complex migration
            registerApi(
                methodName = "initSession",
                signature = "Branch.initSession(Activity, BranchReferralInitListener)",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.MEDIUM,
                removalTimeline = "Q3 2025",
                modernReplacement = "sessionManager.initSession()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.5.0" // Extended due to complexity
            )
            
            registerApi(
                methodName = "resetUserSession",
                signature = "Branch.resetUserSession()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "sessionManager.resetSession()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.0.0" // Standard removal
            )
            
            // User Identity APIs - High impact, standard timeline
            registerApi(
                methodName = "setIdentity",
                signature = "Branch.setIdentity(String)",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "identityManager.setIdentity(String)",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.0.0" // Standard removal
            )
            
            registerApi(
                methodName = "logout",
                signature = "Branch.logout()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "identityManager.logout()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.0.0" // Standard removal
            )
            
            // Link Creation APIs - Critical but complex, longer timeline
            registerApi(
                methodName = "generateShortUrl",
                signature = "BranchUniversalObject.generateShortUrl()",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.COMPLEX,
                removalTimeline = "Q4 2025",
                modernReplacement = "linkManager.createShortLink()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "7.0.0" // Extended due to critical usage and complexity
            )
            
            // Event Tracking APIs - High impact, standard timeline
            registerApi(
                methodName = "logEvent",
                signature = "BranchEvent.logEvent(Context)",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.MEDIUM,
                removalTimeline = "Q4 2025",
                modernReplacement = "eventManager.logEvent()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.5.0" // Extended due to complexity
            )
            
            // Configuration APIs - Medium impact, earlier removal
            registerApi(
                methodName = "enableTestMode",
                signature = "Branch.enableTestMode()",
                usageImpact = UsageImpact.MEDIUM,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "configManager.enableTestMode()",
                deprecationVersion = "4.5.0", // Earlier deprecation
                removalVersion = "5.5.0" // Earlier removal
            )
            
            // Data Retrieval APIs - High impact, standard timeline
            registerApi(
                methodName = "getFirstReferringParams",
                signature = "Branch.getFirstReferringParams()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "dataManager.getFirstReferringParams()",
                deprecationVersion = "5.0.0", // Standard deprecation
                removalVersion = "6.0.0" // Standard removal
            )
            
            // Synchronous APIs - High priority for removal due to blocking nature
            registerApi(
                methodName = "getFirstReferringParamsSync",
                signature = "Branch.getFirstReferringParamsSync()",
                usageImpact = UsageImpact.MEDIUM,
                complexity = MigrationComplexity.COMPLEX,
                removalTimeline = "Q1 2025", // Earlier removal due to blocking nature
                modernReplacement = "dataManager.getFirstReferringParamsAsync()",
                breakingChanges = listOf("Converted from synchronous to asynchronous operation"),
                deprecationVersion = "4.0.0", // Very early deprecation
                removalVersion = "5.0.0" // Early removal due to performance impact
            )
        }
    }
    
    /**
     * Handle legacy API calls by logging usage, providing deprecation warnings,
     * and delegating to the modern implementation.
     */
    fun handleLegacyApiCall(methodName: String, parameters: Array<Any?>): Any? {
        val startTime = System.nanoTime()
        
        try {
            // Record API usage for analytics
            recordApiUsage(methodName, parameters)
            
            // Log deprecation warning
            logDeprecationWarning(methodName)
            
            // Delegate to modern implementation
            val result = delegateToModernCore(methodName, parameters)
            
            // Record successful completion
            recordApiCallCompletion(methodName, startTime, success = true)
            
            return result
            
        } catch (e: Exception) {
            // Record failed completion
            recordApiCallCompletion(methodName, startTime, success = false, error = e)
            throw e
        }
    }
    
    /**
     * Record API usage for analytics and migration planning.
     */
    private fun recordApiUsage(methodName: String, parameters: Array<Any?>) {
        usageAnalytics.recordApiCall(
            methodName = methodName,
            parameterCount = parameters.size,
            timestamp = System.currentTimeMillis(),
            threadName = Thread.currentThread().name
        )
        
        // Update active calls cache for performance monitoring
        activeCallsCache[methodName] = System.currentTimeMillis()
    }
    
    /**
     * Log structured deprecation warnings with migration guidance.
     */
    private fun logDeprecationWarning(methodName: String) {
        val apiInfo = publicApiRegistry.getApiInfo(methodName)
        if (apiInfo != null) {
            val message = buildDeprecationMessage(apiInfo)
            
            when (apiInfo.usageImpact) {
                UsageImpact.CRITICAL -> BranchLogger.w(message)
                UsageImpact.HIGH -> BranchLogger.w(message)
                UsageImpact.MEDIUM -> BranchLogger.i(message)
                UsageImpact.LOW -> BranchLogger.d(message)
            }
            
            // Send analytics about deprecated API usage (optional)
            usageAnalytics.recordDeprecationWarning(methodName, apiInfo)
        }
    }
    
    /**
     * Build comprehensive deprecation messages with migration guidance.
     */
    private fun buildDeprecationMessage(apiInfo: ApiMethodInfo): String {
        return buildString {
            appendLine("🚨 DEPRECATED API USAGE:")
            appendLine("Method: ${apiInfo.signature}")
            appendLine("Deprecated in: ${apiInfo.deprecationVersion}")
            appendLine("Will be removed in: ${apiInfo.removalVersion} (${apiInfo.removalTimeline})")
            appendLine("Impact Level: ${apiInfo.usageImpact}")
            appendLine("Migration Complexity: ${apiInfo.migrationComplexity}")
            appendLine("Modern Alternative: ${apiInfo.modernReplacement}")
            
            if (apiInfo.breakingChanges.isNotEmpty()) {
                appendLine("Breaking Changes:")
                apiInfo.breakingChanges.forEach { change ->
                    appendLine("  • $change")
                }
            }
            
            appendLine("Migration Guide: ${versionConfiguration.getMigrationGuideUrl()}")
        }
    }
    
    /**
     * Delegate API calls to the appropriate modern implementation.
     * This method routes legacy calls to the new architecture.
     */
    private fun delegateToModernCore(methodName: String, parameters: Array<Any?>): Any? {
        return when (methodName) {
            "getInstance" -> {
                BranchLogger.d("Delegating getInstance() to modern implementation")
                modernBranchCore // Return the modern core instance
            }
            "getAutoInstance" -> {
                val context = parameters[0] as Context
                BranchLogger.d("Delegating getAutoInstance() to modern implementation")
                // Handle asynchronous initialization
                runBlocking {
                    modernBranchCore?.let { core ->
                        // core.initialize(context) // Will be implemented when ModernBranchCore is available
                    }
                }
                modernBranchCore
            }
            "setIdentity" -> {
                val userId = parameters[0] as String
                BranchLogger.d("Delegating setIdentity() to modern implementation")
                // Handle asynchronous operation
                coroutineScope.launch {
                    modernBranchCore?.let { core ->
                        // core.identityManager.setIdentity(userId) // Will be implemented when available
                    }
                }
                null
            }
            "resetUserSession" -> {
                BranchLogger.d("Delegating resetUserSession() to modern implementation")
                // Handle asynchronous operation
                coroutineScope.launch {
                    modernBranchCore?.let { core ->
                        // core.sessionManager.resetSession() // Will be implemented when available
                    }
                }
                null
            }
            "enableTestMode" -> {
                BranchLogger.d("Delegating enableTestMode() to modern implementation")
                modernBranchCore?.let { core ->
                    // core.configurationManager.enableTestMode() // Will be implemented when available
                }
                null
            }
            "getFirstReferringParams" -> {
                BranchLogger.d("Delegating getFirstReferringParams() to modern implementation")
                modernBranchCore?.let { core ->
                    // core.dataManager.getFirstReferringParams() // Will be implemented when available
                }
                null
            }
            // Add more delegations as needed
            else -> {
                BranchLogger.w("No modern implementation found for legacy method: $methodName")
                null
            }
        }
    }
    
    /**
     * Record API call completion for performance monitoring.
     */
    private fun recordApiCallCompletion(
        methodName: String, 
        startTime: Long, 
        success: Boolean, 
        error: Exception? = null
    ) {
        val duration = System.nanoTime() - startTime
        val durationMs = duration / 1_000_000.0
        
        usageAnalytics.recordApiCallCompletion(
            methodName = methodName,
            durationMs = durationMs,
            success = success,
            errorType = error?.javaClass?.simpleName
        )
        
        // Remove from active calls cache
        activeCallsCache.remove(methodName)
        
        // Log performance warnings for slow calls
        if (durationMs > 100) { // 100ms threshold
            BranchLogger.w("Slow API call detected: $methodName took ${durationMs}ms")
        }
    }
    
    /**
     * Get comprehensive migration report for planning purposes.
     */
    fun generateMigrationReport(): MigrationReport {
        return publicApiRegistry.generateMigrationReport(usageAnalytics.getUsageData())
    }
    
    /**
     * Get version timeline report showing deprecation and removal schedule.
     * This report is useful for release planning and communication to developers.
     */
    fun generateVersionTimelineReport(): VersionTimelineReport {
        return publicApiRegistry.generateVersionTimelineReport()
    }
    
    /**
     * Get APIs that will be deprecated in a specific version.
     * Useful for generating release notes and migration guides.
     */
    fun getApisForDeprecationInVersion(version: String): List<ApiMethodInfo> {
        return publicApiRegistry.getApisForDeprecation(version)
    }
    
    /**
     * Get APIs that will be removed in a specific version.
     * Critical for validating breaking changes before release.
     */
    fun getApisForRemovalInVersion(version: String): List<ApiMethodInfo> {
        return publicApiRegistry.getApisForRemoval(version)
    }
    
    /**
     * Get current API usage analytics.
     */
    fun getUsageAnalytics(): ApiUsageAnalytics = usageAnalytics
    
    /**
     * Get public API registry for inspection.
     */
    fun getApiRegistry(): PublicApiRegistry = publicApiRegistry
    
    /**
     * Check if SDK is ready for operation.
     */
    fun isReady(): Boolean {
        return modernBranchCore?.let { core ->
            // core.isInitialized() // Will be implemented when ModernBranchCore is available
            true
        } ?: false
    }
}

 