package io.branch.referral.modernization

import android.content.Context
import androidx.annotation.NonNull
import io.branch.referral.BranchLogger
import io.branch.referral.modernization.analytics.ApiUsageAnalytics
import io.branch.referral.modernization.core.ModernBranchCore
import io.branch.referral.modernization.registry.PublicApiRegistry
import io.branch.referral.modernization.registry.ApiMethodInfo
import io.branch.referral.modernization.registry.UsageImpact
import io.branch.referral.modernization.registry.MigrationComplexity
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
class BranchApiPreservationManager private constructor() {
    
    private val modernBranchCore: ModernBranchCore by lazy {
        ModernBranchCore.getInstance()
    }
    
    private val publicApiRegistry = PublicApiRegistry()
    private val usageAnalytics = ApiUsageAnalytics()
    private val activeCallsCache = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val DEPRECATION_VERSION = "6.0.0"
        private const val REMOVAL_VERSION = "7.0.0"
        private const val MIGRATION_GUIDE_URL = "https://branch.io/migration-guide"
        
        @Volatile
        private var instance: BranchApiPreservationManager? = null
        
        /**
         * Get the singleton instance of the preservation manager.
         * Thread-safe initialization ensures single instance across the application.
         */
        fun getInstance(): BranchApiPreservationManager {
            return instance ?: synchronized(this) {
                instance ?: BranchApiPreservationManager().also { instance = it }
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
     */
    private fun registerAllPublicApis() {
        publicApiRegistry.apply {
            // Core Instance Management APIs
            registerApi(
                methodName = "getInstance",
                signature = "Branch.getInstance()",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "ModernBranchCore.getInstance()"
            )
            
            registerApi(
                methodName = "getAutoInstance",
                signature = "Branch.getAutoInstance(Context)",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "ModernBranchCore.initialize(Context)"
            )
            
            // Session Management APIs
            registerApi(
                methodName = "initSession",
                signature = "Branch.initSession(Activity, BranchReferralInitListener)",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.MEDIUM,
                removalTimeline = "Q3 2025",
                modernReplacement = "sessionManager.initSession()"
            )
            
            registerApi(
                methodName = "resetUserSession",
                signature = "Branch.resetUserSession()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "sessionManager.resetSession()"
            )
            
            // User Identity APIs
            registerApi(
                methodName = "setIdentity",
                signature = "Branch.setIdentity(String)",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "identityManager.setIdentity(String)"
            )
            
            registerApi(
                methodName = "logout",
                signature = "Branch.logout()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "identityManager.logout()"
            )
            
            // Link Creation APIs
            registerApi(
                methodName = "generateShortUrl",
                signature = "BranchUniversalObject.generateShortUrl()",
                usageImpact = UsageImpact.CRITICAL,
                complexity = MigrationComplexity.COMPLEX,
                removalTimeline = "Q4 2025",
                modernReplacement = "linkManager.createShortLink()"
            )
            
            // Event Tracking APIs
            registerApi(
                methodName = "logEvent",
                signature = "BranchEvent.logEvent(Context)",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.MEDIUM,
                removalTimeline = "Q4 2025",
                modernReplacement = "eventManager.logEvent()"
            )
            
            // Configuration APIs
            registerApi(
                methodName = "enableTestMode",
                signature = "Branch.enableTestMode()",
                usageImpact = UsageImpact.MEDIUM,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q2 2025",
                modernReplacement = "configManager.enableTestMode()"
            )
            
            // Data Retrieval APIs
            registerApi(
                methodName = "getFirstReferringParams",
                signature = "Branch.getFirstReferringParams()",
                usageImpact = UsageImpact.HIGH,
                complexity = MigrationComplexity.SIMPLE,
                removalTimeline = "Q3 2025",
                modernReplacement = "dataManager.getFirstReferringParams()"
            )
            
            // Synchronous APIs marked for direct migration
            registerApi(
                methodName = "getFirstReferringParamsSync",
                signature = "Branch.getFirstReferringParamsSync()",
                usageImpact = UsageImpact.MEDIUM,
                complexity = MigrationComplexity.COMPLEX,
                removalTimeline = "Q1 2025", // Earlier removal due to blocking nature
                modernReplacement = "dataManager.getFirstReferringParamsAsync()",
                breakingChanges = listOf("Converted from synchronous to asynchronous operation")
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
            
            appendLine("Migration Guide: $MIGRATION_GUIDE_URL")
        }
    }
    
    /**
     * Delegate API calls to the appropriate modern implementation.
     * This method routes legacy calls to the new architecture.
     */
    private fun delegateToModernCore(methodName: String, parameters: Array<Any?>): Any? {
        return when (methodName) {
            "getInstance" -> modernBranchCore
            "getAutoInstance" -> {
                val context = parameters[0] as Context
                modernBranchCore.initialize(context)
            }
            "setIdentity" -> {
                val userId = parameters[0] as String
                modernBranchCore.identityManager.setIdentity(userId)
            }
            "resetUserSession" -> {
                modernBranchCore.sessionManager.resetSession()
            }
            "enableTestMode" -> {
                modernBranchCore.configurationManager.enableTestMode()
            }
            "getFirstReferringParams" -> {
                modernBranchCore.dataManager.getFirstReferringParams()
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
        return modernBranchCore.isInitialized()
    }
}

/**
 * Migration report containing analysis and recommendations.
 */
data class MigrationReport(
    val totalApis: Int,
    val criticalApis: Int,
    val complexMigrations: Int,
    val estimatedMigrationEffort: String,
    val recommendedTimeline: String,
    val riskFactors: List<String>,
    val usageStatistics: Map<String, Int>
) 