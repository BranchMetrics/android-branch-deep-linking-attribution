package io.branch.referral.modernization.registry

import java.util.concurrent.ConcurrentHashMap

/**
 * Comprehensive registry for all public APIs that must be preserved during modernization.
 * 
 * This registry maintains detailed information about each API method, including:
 * - Usage impact and migration complexity
 * - Deprecation timeline and modern replacements
 * - Breaking changes and migration guidance
 * 
 * Responsibilities:
 * - Catalog all stable public API signatures
 * - Track API metadata for migration planning
 * - Generate migration reports and analytics
 * - Provide deprecation guidance and warnings
 */
class PublicApiRegistry {
    
    private val apiCatalog = ConcurrentHashMap<String, ApiMethodInfo>()
    private val apisByCategory = ConcurrentHashMap<String, MutableList<String>>()
    private val apisByImpact = ConcurrentHashMap<UsageImpact, MutableList<String>>()
    private val apisByComplexity = ConcurrentHashMap<MigrationComplexity, MutableList<String>>()
    
    /**
     * Register a public API method in the preservation catalog.
     */
    fun registerApi(
        methodName: String,
        signature: String,
        usageImpact: UsageImpact,
        complexity: MigrationComplexity,
        removalTimeline: String,
        modernReplacement: String,
        category: String = inferCategory(signature),
        breakingChanges: List<String> = emptyList(),
        migrationNotes: String = ""
    ) {
        val apiInfo = ApiMethodInfo(
            methodName = methodName,
            signature = signature,
            usageImpact = usageImpact,
            migrationComplexity = complexity,
            removalTimeline = removalTimeline,
            modernReplacement = modernReplacement,
            category = category,
            breakingChanges = breakingChanges,
            migrationNotes = migrationNotes,
            deprecationVersion = "5.0.0",
            removalVersion = "6.0.0"
        )
        
        // Register in main catalog
        apiCatalog[methodName] = apiInfo
        
        // Register in categorized indexes
        apisByCategory.getOrPut(category) { mutableListOf() }.add(methodName)
        apisByImpact.getOrPut(usageImpact) { mutableListOf() }.add(methodName)
        apisByComplexity.getOrPut(complexity) { mutableListOf() }.add(methodName)
    }
    
    /**
     * Get API information for a specific method.
     */
    fun getApiInfo(methodName: String): ApiMethodInfo? = apiCatalog[methodName]
    
    /**
     * Get all APIs in a specific category.
     */
    fun getApisByCategory(category: String): List<ApiMethodInfo> {
        return apisByCategory[category]?.mapNotNull { apiCatalog[it] } ?: emptyList()
    }
    
    /**
     * Get all APIs with a specific usage impact level.
     */
    fun getApisByImpact(impact: UsageImpact): List<ApiMethodInfo> {
        return apisByImpact[impact]?.mapNotNull { apiCatalog[it] } ?: emptyList()
    }
    
    /**
     * Get all APIs with a specific migration complexity.
     */
    fun getApisByComplexity(complexity: MigrationComplexity): List<ApiMethodInfo> {
        return apisByComplexity[complexity]?.mapNotNull { apiCatalog[it] } ?: emptyList()
    }
    
    /**
     * Get total number of registered APIs.
     */
    fun getTotalApiCount(): Int = apiCatalog.size
    
    /**
     * Get all API categories.
     */
    fun getAllCategories(): Set<String> = apisByCategory.keys.toSet()
    
    /**
     * Generate comprehensive migration report with analytics.
     */
    fun generateMigrationReport(usageData: Map<String, ApiUsageData>): MigrationReport {
        val totalApis = apiCatalog.size
        val criticalApis = apisByImpact[UsageImpact.CRITICAL]?.size ?: 0
        val complexMigrations = apisByComplexity[MigrationComplexity.COMPLEX]?.size ?: 0
        
        val usageStatistics = usageData.entries.associate { (method, data) ->
            method to data.callCount
        }
        
        val riskFactors = mutableListOf<String>()
        
        // Analyze risk factors
        if (criticalApis > totalApis * 0.5) {
            riskFactors.add("High number of critical APIs (${criticalApis}/${totalApis})")
        }
        
        if (complexMigrations > totalApis * 0.2) {
            riskFactors.add("Significant complex migrations (${complexMigrations}/${totalApis})")
        }
        
        // Check for heavily used deprecated APIs
        val heavilyUsedDeprecated = usageData.entries
            .filter { (method, data) -> data.callCount > 1000 && apiCatalog[method] != null }
            .size
        
        if (heavilyUsedDeprecated > 0) {
            riskFactors.add("$heavilyUsedDeprecated heavily used deprecated APIs detected")
        }
        
        return MigrationReport(
            totalApis = totalApis,
            criticalApis = criticalApis,
            complexMigrations = complexMigrations,
            estimatedMigrationEffort = calculateEffortEstimate(totalApis, complexMigrations),
            recommendedTimeline = calculateRecommendedTimeline(criticalApis, complexMigrations),
            riskFactors = riskFactors,
            usageStatistics = usageStatistics
        )
    }
    
    /**
     * Calculate estimated effort for migration.
     */
    private fun calculateEffortEstimate(totalApis: Int, complexMigrations: Int): String {
        val simpleApis = apisByComplexity[MigrationComplexity.SIMPLE]?.size ?: 0
        val mediumApis = apisByComplexity[MigrationComplexity.MEDIUM]?.size ?: 0
        
        // Effort estimation: Simple=1 day, Medium=3 days, Complex=7 days
        val totalDays = simpleApis * 1 + mediumApis * 3 + complexMigrations * 7
        val totalWeeks = (totalDays / 5.0)
        
        return when {
            totalWeeks < 4 -> "Low effort (${totalWeeks.toInt()} weeks)"
            totalWeeks < 12 -> "Medium effort (${totalWeeks.toInt()} weeks)"
            else -> "High effort (${totalWeeks.toInt()} weeks)"
        }
    }
    
    /**
     * Calculate recommended timeline based on complexity and criticality.
     */
    private fun calculateRecommendedTimeline(criticalApis: Int, complexMigrations: Int): String {
        return when {
            criticalApis > 20 && complexMigrations > 10 -> "24 months (4 phases)"
            criticalApis > 10 || complexMigrations > 5 -> "18 months (3 phases)"
            else -> "12 months (2 phases)"
        }
    }
    
    /**
     * Infer API category from method signature.
     */
    private fun inferCategory(signature: String): String {
        return when {
            signature.contains("Branch.getInstance") -> "Instance Management"
            signature.contains("initSession") -> "Session Management"
            signature.contains("setIdentity") || signature.contains("logout") -> "User Identity"
            signature.contains("generateShortUrl") || signature.contains("createQRCode") -> "Link Generation"
            signature.contains("logEvent") -> "Event Tracking"
            signature.contains("enableTestMode") || signature.contains("setDebug") -> "Configuration"
            signature.contains("getReferringParams") || signature.contains("getFirstReferringParams") -> "Data Retrieval"
            signature.contains("BranchUniversalObject") -> "Universal Objects"
            signature.contains("BranchEvent") -> "Event System"
            signature.contains("LinkProperties") -> "Link Properties"
            else -> "General"
        }
    }
    
    /**
     * Get APIs that should be removed in the next version.
     */
    fun getApisForRemoval(): List<ApiMethodInfo> {
        return apiCatalog.values.filter { api ->
            api.removalTimeline.contains("Q1 2025") // High priority removal
        }
    }
    
    /**
     * Get migration complexity distribution.
     */
    fun getComplexityDistribution(): Map<MigrationComplexity, Int> {
        return MigrationComplexity.values().associateWith { complexity ->
            apisByComplexity[complexity]?.size ?: 0
        }
    }
    
    /**
     * Get impact level distribution.
     */
    fun getImpactDistribution(): Map<UsageImpact, Int> {
        return UsageImpact.values().associateWith { impact ->
            apisByImpact[impact]?.size ?: 0
        }
    }
}

/**
 * Detailed information about a preserved API method.
 */
data class ApiMethodInfo(
    val methodName: String,
    val signature: String,
    val usageImpact: UsageImpact,
    val migrationComplexity: MigrationComplexity,
    val removalTimeline: String,
    val modernReplacement: String,
    val category: String,
    val breakingChanges: List<String>,
    val migrationNotes: String,
    val deprecationVersion: String,
    val removalVersion: String
)

/**
 * Usage impact levels for API preservation planning.
 */
enum class UsageImpact {
    CRITICAL,   // Essential APIs used by majority of applications
    HIGH,       // Important APIs used by many applications
    MEDIUM,     // Moderately used APIs
    LOW         // Rarely used APIs
}

/**
 * Migration complexity levels for effort estimation.
 */
enum class MigrationComplexity {
    SIMPLE,     // Direct wrapper or simple delegation
    MEDIUM,     // Parameter transformation or callback adaptation
    COMPLEX     // Significant architectural changes or breaking changes
}

/**
 * API usage data for analytics and migration planning.
 */
data class ApiUsageData(
    val methodName: String,
    val callCount: Int,
    val lastUsed: Long,
    val averageCallsPerDay: Double,
    val uniqueApplications: Int
)

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