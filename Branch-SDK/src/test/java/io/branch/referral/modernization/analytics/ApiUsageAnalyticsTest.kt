package io.branch.referral.modernization.analytics

import io.branch.referral.modernization.registry.ApiMethodInfo
import io.branch.referral.modernization.registry.UsageImpact
import io.branch.referral.modernization.registry.MigrationComplexity
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Comprehensive unit tests for ApiUsageAnalytics.
 * 
 * Tests all public methods, performance tracking, and analytics generation to achieve 95% code coverage.
 */
class ApiUsageAnalyticsTest {
    
    private lateinit var analytics: ApiUsageAnalytics
    
    @Before
    fun setup() {
        analytics = ApiUsageAnalytics()
    }
    
    @Test
    fun `test recordApiCall`() {
        analytics.recordApiCall("testMethod", 2, System.currentTimeMillis(), "main")
        
        val usageData = analytics.getUsageData()
        assertTrue("Should track method call", usageData.containsKey("testMethod"))
        
        val methodData = usageData["testMethod"]
        assertNotNull("Should have method data", methodData)
        assertEquals("Should have correct call count", 1, methodData.callCount)
    }
    
    @Test
    fun `test recordApiCall with multiple calls`() {
        val timestamp = System.currentTimeMillis()
        
        analytics.recordApiCall("testMethod", 1, timestamp, "main")
        analytics.recordApiCall("testMethod", 2, timestamp + 1000, "background")
        analytics.recordApiCall("testMethod", 0, timestamp + 2000, "main")
        
        val usageData = analytics.getUsageData()
        val methodData = usageData["testMethod"]
        
        assertNotNull("Should have method data", methodData)
        assertEquals("Should have correct call count", 3, methodData.callCount)
        assertTrue("Should track last used time", methodData.lastUsed > 0)
    }
    
    @Test
    fun `test recordApiCallCompletion with success`() {
        analytics.recordApiCall("testMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCallCompletion("testMethod", 150.5, true)
        
        val performance = analytics.getPerformanceAnalytics()
        assertTrue("Should have performance data", performance.totalApiCalls > 0)
        assertTrue("Should have wrapper overhead", performance.totalWrapperOverheadMs > 0)
    }
    
    @Test
    fun `test recordApiCallCompletion with failure`() {
        analytics.recordApiCall("testMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCallCompletion("testMethod", 200.0, false, "NetworkError")
        
        val performance = analytics.getPerformanceAnalytics()
        assertTrue("Should track failed calls", performance.totalApiCalls > 0)
    }
    
    @Test
    fun `test recordDeprecationWarning`() {
        val apiInfo = ApiMethodInfo(
            methodName = "deprecatedMethod",
            signature = "deprecatedMethod()",
            usageImpact = UsageImpact.MEDIUM,
            migrationComplexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod()",
            category = "Test",
            breakingChanges = emptyList(),
            migrationNotes = "",
            deprecationVersion = "5.0.0",
            removalVersion = "6.0.0"
        )
        
        analytics.recordDeprecationWarning("deprecatedMethod", apiInfo)
        
        val deprecationAnalytics = analytics.getDeprecationAnalytics()
        assertTrue("Should track deprecation warnings", deprecationAnalytics.totalDeprecationWarnings > 0)
    }
    
    @Test
    fun `test getUsageData`() {
        val timestamp = System.currentTimeMillis()
        
        analytics.recordApiCall("method1", 1, timestamp, "main")
        analytics.recordApiCall("method2", 2, timestamp + 1000, "background")
        
        val usageData = analytics.getUsageData()
        
        assertTrue("Should have method1", usageData.containsKey("method1"))
        assertTrue("Should have method2", usageData.containsKey("method2"))
        assertEquals("Should have 2 methods", 2, usageData.size)
        
        val method1Data = usageData["method1"]
        assertNotNull("Should have method1 data", method1Data)
        assertEquals("Should have correct method name", "method1", method1Data.methodName)
        assertEquals("Should have correct call count", 1, method1Data.callCount)
        assertTrue("Should have last used time", method1Data.lastUsed > 0)
    }
    
    @Test
    fun `test getUsageData with average calls per day`() {
        val timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000) // 2 days ago
        
        analytics.recordApiCall("testMethod", 1, timestamp, "main")
        analytics.recordApiCall("testMethod", 1, timestamp + 1000, "main")
        
        val usageData = analytics.getUsageData()
        val methodData = usageData["testMethod"]
        
        assertNotNull("Should have method data", methodData)
        assertTrue("Should calculate average calls per day", methodData.averageCallsPerDay > 0)
    }
    
    @Test
    fun `test getPerformanceAnalytics`() {
        analytics.recordApiCall("fastMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCall("slowMethod", 2, System.currentTimeMillis(), "main")
        
        analytics.recordApiCallCompletion("fastMethod", 50.0, true)
        analytics.recordApiCallCompletion("slowMethod", 500.0, true)
        
        val performance = analytics.getPerformanceAnalytics()
        
        assertTrue("Should have total API calls", performance.totalApiCalls > 0)
        assertTrue("Should have wrapper overhead", performance.totalWrapperOverheadMs > 0)
        assertTrue("Should have average overhead", performance.averageWrapperOverheadMs > 0)
        assertTrue("Should have method performance data", performance.methodPerformance.isNotEmpty())
        assertNotNull("Should have slow methods", performance.slowMethods)
    }
    
    @Test
    fun `test getPerformanceAnalytics with method performance`() {
        analytics.recordApiCall("testMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCallCompletion("testMethod", 100.0, true)
        analytics.recordApiCallCompletion("testMethod", 200.0, true)
        analytics.recordApiCallCompletion("testMethod", 300.0, true)
        
        val performance = analytics.getPerformanceAnalytics()
        val methodPerformance = performance.methodPerformance["testMethod"]
        
        assertNotNull("Should have method performance", methodPerformance)
        assertEquals("Should have correct method name", "testMethod", methodPerformance.methodName)
        assertEquals("Should have correct call count", 3, methodPerformance.callCount)
        assertEquals("Should have correct average duration", 200.0, methodPerformance.averageDurationMs, 0.1)
        assertEquals("Should have correct min duration", 100.0, methodPerformance.minDurationMs, 0.1)
        assertEquals("Should have correct max duration", 300.0, methodPerformance.maxDurationMs, 0.1)
    }
    
    @Test
    fun `test getDeprecationAnalytics`() {
        val apiInfo = ApiMethodInfo(
            methodName = "deprecatedMethod",
            signature = "deprecatedMethod()",
            usageImpact = UsageImpact.MEDIUM,
            migrationComplexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod()",
            category = "Test",
            breakingChanges = emptyList(),
            migrationNotes = "",
            deprecationVersion = "5.0.0",
            removalVersion = "6.0.0"
        )
        
        analytics.recordDeprecationWarning("deprecatedMethod", apiInfo)
        analytics.recordDeprecationWarning("deprecatedMethod", apiInfo)
        analytics.recordApiCall("deprecatedMethod", 1, System.currentTimeMillis(), "main")
        
        val deprecationAnalytics = analytics.getDeprecationAnalytics()
        
        assertEquals("Should have correct total warnings", 2, deprecationAnalytics.totalDeprecationWarnings)
        assertEquals("Should have correct methods with warnings", 1, deprecationAnalytics.methodsWithWarnings)
        assertEquals("Should have correct deprecated API calls", 1, deprecationAnalytics.totalDeprecatedApiCalls)
        assertTrue("Should have most used deprecated APIs", deprecationAnalytics.mostUsedDeprecatedApis.isNotEmpty())
    }
    
    @Test
    fun `test getThreadAnalytics`() {
        analytics.recordApiCall("mainMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCall("backgroundMethod", 1, System.currentTimeMillis(), "background")
        analytics.recordApiCall("mainMethod", 1, System.currentTimeMillis(), "main")
        
        val threadAnalytics = analytics.getThreadAnalytics()
        
        assertTrue("Should have method thread usage", threadAnalytics.methodThreadUsage.isNotEmpty())
        assertTrue("Should have main thread methods", threadAnalytics.mainThreadMethods.isNotEmpty())
        assertNotNull("Should have potential threading issues", threadAnalytics.potentialThreadingIssues)
        
        val mainMethodThreads = threadAnalytics.methodThreadUsage["mainMethod"]
        assertNotNull("Should have main method threads", mainMethodThreads)
        assertTrue("Should contain main thread", mainMethodThreads.contains("main"))
    }
    
    @Test
    fun `test generateMigrationInsights`() {
        val timestamp = System.currentTimeMillis()
        
        // Add high usage method
        repeat(150) {
            analytics.recordApiCall("highUsageMethod", 1, timestamp + it, "main")
        }
        
        // Add recently used method
        analytics.recordApiCall("recentMethod", 1, timestamp, "main")
        
        // Add slow method
        analytics.recordApiCall("slowMethod", 1, timestamp, "main")
        analytics.recordApiCallCompletion("slowMethod", 1000.0, true)
        
        val insights = analytics.generateMigrationInsights()
        
        assertTrue("Should have priority methods", insights.priorityMethods.isNotEmpty())
        assertTrue("Should have recently active methods", insights.recentlyActiveMethods.isNotEmpty())
        assertNotNull("Should have performance concerns", insights.performanceConcerns)
        assertNotNull("Should have recommended migration order", insights.recommendedMigrationOrder)
        
        assertTrue("Should include high usage method in priority", 
                   insights.priorityMethods.contains("highUsageMethod"))
        assertTrue("Should include recent method in recently active", 
                   insights.recentlyActiveMethods.contains("recentMethod"))
    }
    
    @Test
    fun `test reset functionality`() {
        analytics.recordApiCall("testMethod", 1, System.currentTimeMillis(), "main")
        analytics.recordApiCallCompletion("testMethod", 100.0, true)
        
        val usageDataBefore = analytics.getUsageData()
        assertTrue("Should have data before reset", usageDataBefore.isNotEmpty())
        
        analytics.reset()
        
        val usageDataAfter = analytics.getUsageData()
        assertTrue("Should be empty after reset", usageDataAfter.isEmpty())
        
        val performanceAfter = analytics.getPerformanceAnalytics()
        assertEquals("Should have zero calls after reset", 0, performanceAfter.totalApiCalls)
        assertEquals("Should have zero overhead after reset", 0, performanceAfter.totalWrapperOverheadMs)
    }
    
    @Test
    fun `test concurrent access`() {
        val latch = CountDownLatch(3)
        val analytics = ApiUsageAnalytics()
        
        Thread {
            repeat(10) {
                analytics.recordApiCall("method1", 1, System.currentTimeMillis(), "thread1")
                analytics.recordApiCallCompletion("method1", 100.0, true)
            }
            latch.countDown()
        }.start()
        
        Thread {
            repeat(10) {
                analytics.recordApiCall("method2", 1, System.currentTimeMillis(), "thread2")
                analytics.recordApiCallCompletion("method2", 200.0, true)
            }
            latch.countDown()
        }.start()
        
        Thread {
            repeat(10) {
                analytics.recordApiCall("method3", 1, System.currentTimeMillis(), "thread3")
                analytics.recordApiCallCompletion("method3", 300.0, true)
            }
            latch.countDown()
        }.start()
        
        latch.await(5, TimeUnit.SECONDS)
        
        val usageData = analytics.getUsageData()
        assertEquals("Should have 3 methods", 3, usageData.size)
        
        val performance = analytics.getPerformanceAnalytics()
        assertEquals("Should have 30 total calls", 30, performance.totalApiCalls)
    }
    
    @Test
    fun `test edge cases`() {
        // Test with empty method name
        analytics.recordApiCall("", 0, System.currentTimeMillis(), "main")
        
        // Test with null thread name
        analytics.recordApiCall("testMethod", 1, System.currentTimeMillis(), null)
        
        // Test with negative duration
        analytics.recordApiCallCompletion("testMethod", -100.0, true)
        
        // Test with zero duration
        analytics.recordApiCallCompletion("testMethod", 0.0, true)
        
        val usageData = analytics.getUsageData()
        assertTrue("Should handle edge cases gracefully", usageData.isNotEmpty())
    }
    
    @Test
    fun `test performance analytics with no calls`() {
        val performance = analytics.getPerformanceAnalytics()
        
        assertEquals("Should have zero calls", 0, performance.totalApiCalls)
        assertEquals("Should have zero overhead", 0, performance.totalWrapperOverheadMs)
        assertEquals("Should have zero average overhead", 0.0, performance.averageWrapperOverheadMs, 0.1)
        assertTrue("Should have empty method performance", performance.methodPerformance.isEmpty())
        assertTrue("Should have empty slow methods", performance.slowMethods.isEmpty())
    }
    
    @Test
    fun `test deprecation analytics with no warnings`() {
        val deprecationAnalytics = analytics.getDeprecationAnalytics()
        
        assertEquals("Should have zero warnings", 0, deprecationAnalytics.totalDeprecationWarnings)
        assertEquals("Should have zero methods with warnings", 0, deprecationAnalytics.methodsWithWarnings)
        assertEquals("Should have zero deprecated API calls", 0, deprecationAnalytics.totalDeprecatedApiCalls)
        assertTrue("Should have empty most used deprecated APIs", deprecationAnalytics.mostUsedDeprecatedApis.isEmpty())
    }
    
    @Test
    fun `test thread analytics with no calls`() {
        val threadAnalytics = analytics.getThreadAnalytics()
        
        assertTrue("Should have empty method thread usage", threadAnalytics.methodThreadUsage.isEmpty())
        assertTrue("Should have empty main thread methods", threadAnalytics.mainThreadMethods.isEmpty())
        assertTrue("Should have empty potential threading issues", threadAnalytics.potentialThreadingIssues.isEmpty())
    }
    
    @Test
    fun `test migration insights with no data`() {
        val insights = analytics.generateMigrationInsights()
        
        assertTrue("Should have empty priority methods", insights.priorityMethods.isEmpty())
        assertTrue("Should have empty recently active methods", insights.recentlyActiveMethods.isEmpty())
        assertTrue("Should have empty performance concerns", insights.performanceConcerns.isEmpty())
        assertTrue("Should have empty recommended migration order", insights.recommendedMigrationOrder.isEmpty())
    }
    
    @Test
    fun `test average calls per day calculation`() {
        val timestamp = System.currentTimeMillis()
        
        // Single call today
        analytics.recordApiCall("todayMethod", 1, timestamp, "main")
        
        val usageData = analytics.getUsageData()
        val todayMethodData = usageData["todayMethod"]
        
        assertNotNull("Should have method data", todayMethodData)
        assertTrue("Should have positive average calls per day", todayMethodData.averageCallsPerDay > 0)
    }
    
    @Test
    fun `test percentile calculation`() {
        val durations = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
        
        // This tests the private calculatePercentile method indirectly through performance analytics
        analytics.recordApiCall("percentileMethod", 1, System.currentTimeMillis(), "main")
        durations.forEach { duration ->
            analytics.recordApiCallCompletion("percentileMethod", duration, true)
        }
        
        val performance = analytics.getPerformanceAnalytics()
        val methodPerformance = performance.methodPerformance["percentileMethod"]
        
        assertNotNull("Should have method performance", methodPerformance)
        assertTrue("Should calculate p95", methodPerformance.p95DurationMs > 0)
        assertTrue("Should calculate p99", methodPerformance.p99DurationMs > 0)
    }
} 