package io.branch.referral.modernization.analytics

import io.branch.referral.modernization.registry.ApiMethodInfo
import io.branch.referral.modernization.registry.ApiUsageData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Comprehensive analytics system for tracking API usage during modernization.
 * 
 * This system provides detailed metrics about deprecated API usage patterns,
 * performance impact, and migration progress to support data-driven decisions.
 * 
 * Responsibilities:
 * - Track API call frequencies and patterns
 * - Monitor performance impact of wrapper layer
 * - Analyze migration progress and adoption
 * - Generate actionable insights for planning
 */
class ApiUsageAnalytics {
    
    private val callCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val callDurations = ConcurrentHashMap<String, MutableList<Double>>()
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val firstCallTimestamps = ConcurrentHashMap<String, Long>()
    private val lastCallTimestamps = ConcurrentHashMap<String, Long>()
    private val deprecationWarnings = ConcurrentHashMap<String, AtomicInteger>()
    private val threadUsagePatterns = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Performance tracking
    private val totalWrapperOverhead = AtomicLong(0L)
    private val totalDirectCalls = AtomicLong(0L)
    
    /**
     * Record an API call for usage tracking.
     */
    fun recordApiCall(
        methodName: String,
        parameterCount: Int,
        timestamp: Long,
        threadName: String
    ) {
        // Update call count
        callCounts.getOrPut(methodName) { AtomicInteger(0) }.incrementAndGet()
        
        // Track timestamps
        firstCallTimestamps.putIfAbsent(methodName, timestamp)
        lastCallTimestamps[methodName] = timestamp
        
        // Track thread usage patterns
        threadUsagePatterns.getOrPut(methodName) { ConcurrentHashMap.newKeySet() }.add(threadName)
        
        totalDirectCalls.incrementAndGet()
    }
    
    /**
     * Record API call completion with performance metrics.
     */
    fun recordApiCallCompletion(
        methodName: String,
        durationMs: Double,
        success: Boolean,
        errorType: String? = null
    ) {
        // Track performance
        callDurations.getOrPut(methodName) { mutableListOf() }.add(durationMs)
        totalWrapperOverhead.addAndGet(durationMs.toLong())
        
        // Track errors
        if (!success) {
            errorCounts.getOrPut(methodName) { AtomicInteger(0) }.incrementAndGet()
        }
    }
    
    /**
     * Record deprecation warning shown to developer.
     */
    fun recordDeprecationWarning(methodName: String, apiInfo: ApiMethodInfo) {
        deprecationWarnings.getOrPut(methodName) { AtomicInteger(0) }.incrementAndGet()
    }
    
    /**
     * Get comprehensive usage data for all tracked APIs.
     */
    fun getUsageData(): Map<String, ApiUsageData> {
        return callCounts.keys.associateWith { methodName ->
            val callCount = callCounts[methodName]?.get() ?: 0
            val lastUsed = lastCallTimestamps[methodName] ?: 0L
            val firstUsed = firstCallTimestamps[methodName] ?: 0L
            
            val averageCallsPerDay = if (firstUsed > 0) {
                val daysSinceFirst = (System.currentTimeMillis() - firstUsed) / (24 * 60 * 60 * 1000.0)
                if (daysSinceFirst > 0) callCount / daysSinceFirst else callCount.toDouble()
            } else 0.0
            
            ApiUsageData(
                methodName = methodName,
                callCount = callCount,
                lastUsed = lastUsed,
                averageCallsPerDay = averageCallsPerDay,
                uniqueApplications = 1 // Simplified for single app context
            )
        }
    }
    
    /**
     * Get performance analytics for the wrapper layer.
     */
    fun getPerformanceAnalytics(): PerformanceAnalytics {
        val methodPerformance = callDurations.entries.associate { (method, durations) ->
            method to MethodPerformance(
                methodName = method,
                callCount = durations.size,
                averageDurationMs = durations.average(),
                minDurationMs = durations.minOrNull() ?: 0.0,
                maxDurationMs = durations.maxOrNull() ?: 0.0,
                p95DurationMs = calculatePercentile(durations, 0.95),
                p99DurationMs = calculatePercentile(durations, 0.99)
            )
        }
        
        val totalCalls = totalDirectCalls.get()
        val totalOverheadMs = totalWrapperOverhead.get()
        val averageOverheadMs = if (totalCalls > 0) totalOverheadMs.toDouble() / totalCalls else 0.0
        
        return PerformanceAnalytics(
            totalApiCalls = totalCalls,
            totalWrapperOverheadMs = totalOverheadMs,
            averageWrapperOverheadMs = averageOverheadMs,
            methodPerformance = methodPerformance,
            slowMethods = identifySlowMethods(methodPerformance)
        )
    }
    
    /**
     * Get deprecation analytics.
     */
    fun getDeprecationAnalytics(): DeprecationAnalytics {
        val totalWarnings = deprecationWarnings.values.sumOf { it.get() }
        val methodsWithWarnings = deprecationWarnings.size
        val totalDeprecatedCalls = callCounts.entries
            .filter { (method, _) -> deprecationWarnings.containsKey(method) }
            .sumOf { (_, count) -> count.get() }
        
        return DeprecationAnalytics(
            totalDeprecationWarnings = totalWarnings,
            methodsWithWarnings = methodsWithWarnings,
            totalDeprecatedApiCalls = totalDeprecatedCalls,
            mostUsedDeprecatedApis = getMostUsedDeprecatedApis()
        )
    }
    
    /**
     * Get thread usage analytics.
     */
    fun getThreadAnalytics(): ThreadAnalytics {
        val methodThreadUsage = threadUsagePatterns.entries.associate { (method, threads) ->
            method to threads.toList()
        }
        
        val mainThreadUsage = threadUsagePatterns.entries
            .filter { (_, threads) -> threads.any { it.contains("main") } }
            .map { (method, _) -> method }
        
        return ThreadAnalytics(
            methodThreadUsage = methodThreadUsage,
            mainThreadMethods = mainThreadUsage,
            potentialThreadingIssues = identifyThreadingIssues(methodThreadUsage)
        )
    }
    
    /**
     * Generate migration insights based on usage patterns.
     */
    fun generateMigrationInsights(): MigrationInsights {
        val usageData = getUsageData()
        val performanceData = getPerformanceAnalytics()
        
        val highUsageMethods = usageData.entries
            .filter { (_, data) -> data.callCount > 100 }
            .map { (method, _) -> method }
        
        val recentlyUsedMethods = usageData.entries
            .filter { (_, data) -> 
                System.currentTimeMillis() - data.lastUsed < 7 * 24 * 60 * 60 * 1000 // 7 days
            }
            .map { (method, _) -> method }
        
        val slowMethods = performanceData.slowMethods
        
        return MigrationInsights(
            priorityMethods = highUsageMethods,
            recentlyActiveMethods = recentlyUsedMethods,
            performanceConcerns = slowMethods,
            recommendedMigrationOrder = calculateMigrationOrder(usageData, performanceData)
        )
    }
    
    /**
     * Calculate percentile for performance metrics.
     */
    private fun calculatePercentile(values: List<Double>, percentile: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = (percentile * (sorted.size - 1)).toInt()
        return sorted[index]
    }
    
    /**
     * Identify methods with performance issues.
     */
    private fun identifySlowMethods(methodPerformance: Map<String, MethodPerformance>): List<String> {
        return methodPerformance.entries
            .filter { (_, perf) -> perf.averageDurationMs > 50.0 } // 50ms threshold
            .sortedByDescending { (_, perf) -> perf.averageDurationMs }
            .map { (method, _) -> method }
    }
    
    /**
     * Get most frequently used deprecated APIs.
     */
    private fun getMostUsedDeprecatedApis(): List<String> {
        return callCounts.entries
            .filter { (method, _) -> deprecationWarnings.containsKey(method) }
            .sortedByDescending { (_, count) -> count.get() }
            .take(10)
            .map { (method, _) -> method }
    }
    
    /**
     * Identify potential threading issues.
     */
    private fun identifyThreadingIssues(methodThreadUsage: Map<String, List<String>>): List<String> {
        return methodThreadUsage.entries
            .filter { (_, threads) -> threads.size > 3 } // Used on many different threads
            .map { (method, _) -> method }
    }
    
    /**
     * Calculate recommended migration order based on usage and performance.
     */
    private fun calculateMigrationOrder(
        usageData: Map<String, ApiUsageData>,
        performanceData: PerformanceAnalytics
    ): List<String> {
        return usageData.entries
            .sortedWith(compareByDescending<Map.Entry<String, ApiUsageData>> { (_, data) -> 
                data.averageCallsPerDay 
            }.thenByDescending { (method, _) ->
                performanceData.methodPerformance[method]?.averageDurationMs ?: 0.0
            })
            .map { (method, _) -> method }
    }
    
    /**
     * Reset all analytics data (for testing or cleanup).
     */
    fun reset() {
        callCounts.clear()
        callDurations.clear()
        errorCounts.clear()
        firstCallTimestamps.clear()
        lastCallTimestamps.clear()
        deprecationWarnings.clear()
        threadUsagePatterns.clear()
        totalWrapperOverhead.set(0L)
        totalDirectCalls.set(0L)
    }
}

/**
 * Performance analytics for the wrapper layer.
 */
data class PerformanceAnalytics(
    val totalApiCalls: Long,
    val totalWrapperOverheadMs: Long,
    val averageWrapperOverheadMs: Double,
    val methodPerformance: Map<String, MethodPerformance>,
    val slowMethods: List<String>
)

/**
 * Performance metrics for individual methods.
 */
data class MethodPerformance(
    val methodName: String,
    val callCount: Int,
    val averageDurationMs: Double,
    val minDurationMs: Double,
    val maxDurationMs: Double,
    val p95DurationMs: Double,
    val p99DurationMs: Double
)

/**
 * Deprecation usage analytics.
 */
data class DeprecationAnalytics(
    val totalDeprecationWarnings: Int,
    val methodsWithWarnings: Int,
    val totalDeprecatedApiCalls: Int,
    val mostUsedDeprecatedApis: List<String>
)

/**
 * Thread usage analytics.
 */
data class ThreadAnalytics(
    val methodThreadUsage: Map<String, List<String>>,
    val mainThreadMethods: List<String>,
    val potentialThreadingIssues: List<String>
)

/**
 * Migration insights based on usage patterns.
 */
data class MigrationInsights(
    val priorityMethods: List<String>,
    val recentlyActiveMethods: List<String>,
    val performanceConcerns: List<String>,
    val recommendedMigrationOrder: List<String>
) 