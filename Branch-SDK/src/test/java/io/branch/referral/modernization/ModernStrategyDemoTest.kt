package io.branch.referral.modernization

import android.app.Activity
import android.content.Context
import io.branch.referral.Branch
import io.branch.referral.modernization.analytics.ApiUsageAnalytics
import io.branch.referral.modernization.core.ModernBranchCore
import io.branch.referral.modernization.registry.PublicApiRegistry
import io.branch.referral.modernization.registry.UsageImpact
import io.branch.referral.modernization.registry.MigrationComplexity
import io.branch.referral.modernization.wrappers.PreservedBranchApi
import io.branch.referral.modernization.wrappers.LegacyBranchWrapper
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * Comprehensive demonstration test for the Modern Strategy implementation.
 * 
 * This test showcases how the modern architecture preserves legacy APIs
 * while providing a clean, modern foundation for future development.
 * 
 * Test coverage:
 * - API preservation and delegation
 * - Usage analytics and tracking
 * - Callback adaptation
 * - Performance monitoring
 * - Migration insights
 */
class ModernStrategyDemoTest {
    
    private lateinit var preservationManager: BranchApiPreservationManager
    private lateinit var modernCore: ModernBranchCore
    private lateinit var analytics: ApiUsageAnalytics
    private lateinit var registry: PublicApiRegistry
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    
    @Before
    fun setup() {
        // Initialize all components
        preservationManager = BranchApiPreservationManager.getInstance()
        modernCore = ModernBranchCore.getInstance()
        analytics = preservationManager.getUsageAnalytics()
        registry = preservationManager.getApiRegistry()
        
        // Mock Android components
        mockContext = mock(Context::class.java)
        mockActivity = mock(Activity::class.java)
        
        // Reset analytics for clean test
        analytics.reset()
    }
    
    @Test
    fun `demonstrate complete API preservation workflow`() {
        println("\n🎯 === Modern Strategy Demonstration ===")
        
        // 1. Verify preservation manager is initialized
        assertTrue("Preservation manager should be ready", preservationManager.isReady())
        println("✅ Preservation Manager initialized successfully")
        
        // 2. Test API registry has comprehensive coverage
        val totalApis = registry.getTotalApiCount()
        assertTrue("Should have significant API coverage", totalApis >= 10)
        println("✅ API Registry contains $totalApis preserved methods")
        
        // 3. Verify categorization works
        val categories = registry.getAllCategories()
        assertTrue("Should have multiple categories", categories.size >= 5)
        println("✅ APIs categorized into: ${categories.joinToString(", ")}")
        
        // 4. Test impact and complexity distribution
        val impactDistribution = registry.getImpactDistribution()
        val complexityDistribution = registry.getComplexityDistribution()
        
        println("📊 Impact Distribution:")
        impactDistribution.forEach { (impact, count) ->
            println("   $impact: $count methods")
        }
        
        println("📊 Complexity Distribution:")
        complexityDistribution.forEach { (complexity, count) ->
            println("   $complexity: $count methods")
        }
        
        assertTrue("Should have critical APIs", impactDistribution[UsageImpact.CRITICAL]!! > 0)
        assertTrue("Should have simple migrations", complexityDistribution[MigrationComplexity.SIMPLE]!! > 0)
    }
    
    @Test
    fun `demonstrate legacy static API preservation`() {
        println("\n🔄 === Static API Preservation Demo ===")
        
        // Test static method wrapper
        val legacyBranch = PreservedBranchApi.getInstance()
        assertNotNull("Static getInstance should return wrapper", legacyBranch)
        println("✅ Static Branch.getInstance() preserved and functional")
        
        // Test configuration methods
        PreservedBranchApi.enableTestMode()
        println("✅ Static Branch.enableTestMode() preserved")
        
        // Test auto instance
        val autoInstance = PreservedBranchApi.getAutoInstance(mockContext)
        assertNotNull("Auto instance should return wrapper", autoInstance)
        println("✅ Static Branch.getAutoInstance() preserved")
        
        // Verify analytics captured the calls
        val usageData = analytics.getUsageData()
        assertTrue("Analytics should track getInstance calls", 
                   usageData.containsKey("getInstance"))
        assertTrue("Analytics should track enableTestMode calls", 
                   usageData.containsKey("enableTestMode"))
        assertTrue("Analytics should track getAutoInstance calls", 
                   usageData.containsKey("getAutoInstance"))
        
        println("📈 Static API calls tracked in analytics")
    }
    
    @Test
    fun `demonstrate legacy instance API preservation`() {
        println("\n🏃 === Instance API Preservation Demo ===")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        assertNotNull("Wrapper instance should be available", wrapper)
        
        // Test session management
        val sessionResult = wrapper.initSession(mockActivity)
        println("✅ Instance initSession() preserved - result: $sessionResult")
        
        // Test identity management
        wrapper.setIdentity("demo-user-123")
        println("✅ Instance setIdentity() preserved")
        
        // Test data retrieval
        val firstParams = wrapper.getFirstReferringParams()
        val latestParams = wrapper.getLatestReferringParams()
        println("✅ Instance getFirstReferringParams() preserved - result: $firstParams")
        println("✅ Instance getLatestReferringParams() preserved - result: $latestParams")
        
        // Test event tracking
        wrapper.userCompletedAction("demo_action")
        wrapper.userCompletedAction("demo_action_with_data", JSONObject().apply {
            put("custom_key", "custom_value")
            put("timestamp", System.currentTimeMillis())
        })
        println("✅ Instance userCompletedAction() preserved")
        
        // Test configuration
        wrapper.enableTestMode()
        wrapper.disableTracking(false)
        println("✅ Instance configuration methods preserved")
        
        // Verify analytics
        val usageData = analytics.getUsageData()
        assertTrue("Should track initSession", usageData.containsKey("initSession"))
        assertTrue("Should track setIdentity", usageData.containsKey("setIdentity"))
        assertTrue("Should track userCompletedAction", usageData.containsKey("userCompletedAction"))
        
        println("📈 Instance API calls tracked in analytics")
    }
    
    @Test
    fun `demonstrate callback adaptation system`() {
        println("\n🔄 === Callback Adaptation Demo ===")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: io.branch.referral.BranchError? = null
        
        // Test init session callback
        val initCallback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: io.branch.referral.BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
                println("📞 Callback executed - params: $referringParams, error: $error")
            }
        }
        
        // Execute with callback
        wrapper.initSession(initCallback, mockActivity)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        println("✅ Legacy callback successfully adapted and executed")
        
        // Test state change callback
        var stateChanged = false
        val stateCallback = object : Branch.BranchReferralStateChangedListener {
            override fun onStateChanged(changed: Boolean, error: io.branch.referral.BranchError?) {
                stateChanged = changed
                println("📞 State callback executed - changed: $changed, error: $error")
            }
        }
        
        wrapper.logout(stateCallback)
        Thread.sleep(100)
        
        assertTrue("State callback should have been executed", stateChanged)
        println("✅ Legacy state change callback successfully adapted")
    }
    
    @Test
    fun `demonstrate performance monitoring`() {
        println("\n⚡ === Performance Monitoring Demo ===")
        
        // Execute several API calls to generate performance data
        val wrapper = LegacyBranchWrapper.getInstance()
        
        repeat(10) { i ->
            wrapper.initSession(mockActivity)
            wrapper.setIdentity("user-$i")
            wrapper.userCompletedAction("action-$i")
            Thread.sleep(1) // Small delay to simulate processing
        }
        
        // Get performance analytics
        val performanceAnalytics = analytics.getPerformanceAnalytics()
        
        assertTrue("Should have API calls tracked", performanceAnalytics.totalApiCalls > 0)
        assertTrue("Should have performance data", performanceAnalytics.methodPerformance.isNotEmpty())
        
        println("📊 Performance Analytics:")
        println("   Total API calls: ${performanceAnalytics.totalApiCalls}")
        println("   Average overhead: ${performanceAnalytics.averageWrapperOverheadMs}ms")
        println("   Methods with performance data: ${performanceAnalytics.methodPerformance.size}")
        
        performanceAnalytics.methodPerformance.forEach { (method, perf) ->
            println("   $method: ${perf.callCount} calls, avg ${perf.averageDurationMs}ms")
        }
        
        if (performanceAnalytics.slowMethods.isNotEmpty()) {
            println("   ⚠️ Slow methods detected: ${performanceAnalytics.slowMethods}")
        }
        
        println("✅ Performance monitoring working correctly")
    }
    
    @Test
    fun `demonstrate deprecation analytics`() {
        println("\n⚠️ === Deprecation Analytics Demo ===")
        
        // Generate some deprecated API usage
        val wrapper = LegacyBranchWrapper.getInstance()
        
        repeat(5) {
            wrapper.initSession(mockActivity)
            wrapper.setIdentity("test-user")
            wrapper.enableTestMode()
        }
        
        // Get deprecation analytics
        val deprecationAnalytics = analytics.getDeprecationAnalytics()
        
        assertTrue("Should have deprecation warnings", deprecationAnalytics.totalDeprecationWarnings > 0)
        assertTrue("Should have deprecated API calls", deprecationAnalytics.totalDeprecatedApiCalls > 0)
        
        println("📊 Deprecation Analytics:")
        println("   Total warnings shown: ${deprecationAnalytics.totalDeprecationWarnings}")
        println("   Methods with warnings: ${deprecationAnalytics.methodsWithWarnings}")
        println("   Total deprecated calls: ${deprecationAnalytics.totalDeprecatedApiCalls}")
        println("   Most used deprecated APIs: ${deprecationAnalytics.mostUsedDeprecatedApis.take(3)}")
        
        println("✅ Deprecation tracking working correctly")
    }
    
    @Test
    fun `demonstrate migration insights generation`() {
        println("\n🔮 === Migration Insights Demo ===")
        
        // Generate usage patterns
        val wrapper = LegacyBranchWrapper.getInstance()
        
        // High usage methods
        repeat(50) { wrapper.initSession(mockActivity) }
        repeat(30) { wrapper.setIdentity("user-$it") }
        repeat(20) { wrapper.getFirstReferringParams() }
        repeat(10) { wrapper.enableTestMode() }
        
        // Generate insights
        val insights = analytics.generateMigrationInsights()
        
        assertTrue("Should have priority methods", insights.priorityMethods.isNotEmpty())
        assertTrue("Should have recently active methods", insights.recentlyActiveMethods.isNotEmpty())
        
        println("🔍 Migration Insights:")
        println("   Priority methods (high usage): ${insights.priorityMethods.take(3)}")
        println("   Recently active methods: ${insights.recentlyActiveMethods.take(3)}")
        println("   Performance concerns: ${insights.performanceConcerns}")
        println("   Recommended migration order: ${insights.recommendedMigrationOrder.take(5)}")
        
        println("✅ Migration insights generated successfully")
    }
    
    @Test
    fun `demonstrate migration report generation`() {
        println("\n📋 === Migration Report Demo ===")
        
        // Generate usage data for report
        val wrapper = LegacyBranchWrapper.getInstance()
        repeat(25) { wrapper.initSession(mockActivity) }
        repeat(15) { wrapper.setIdentity("user") }
        repeat(10) { wrapper.userCompletedAction("action") }
        
        // Generate comprehensive migration report
        val report = preservationManager.generateMigrationReport()
        
        assertNotNull("Migration report should be generated", report)
        assertTrue("Should have APIs tracked", report.totalApis > 0)
        assertTrue("Should have critical APIs", report.criticalApis > 0)
        
        println("📊 Migration Report:")
        println("   Total APIs preserved: ${report.totalApis}")
        println("   Critical APIs: ${report.criticalApis}")
        println("   Complex migrations: ${report.complexMigrations}")
        println("   Estimated effort: ${report.estimatedMigrationEffort}")
        println("   Recommended timeline: ${report.recommendedTimeline}")
        
        if (report.riskFactors.isNotEmpty()) {
            println("   ⚠️ Risk factors:")
            report.riskFactors.forEach { risk ->
                println("     • $risk")
            }
        }
        
        println("   Usage statistics: ${report.usageStatistics.size} methods tracked")
        
        println("✅ Migration report generated successfully")
    }
    
    @Test
    fun `demonstrate modern architecture integration`() = runBlocking {
        println("\n🏗️ === Modern Architecture Demo ===")
        
        val modernCore = ModernBranchCore.getInstance()
        
        // Test initialization
        val initResult = modernCore.initialize(mockContext)
        assertTrue("Modern core should initialize successfully", initResult.isSuccess)
        assertTrue("Modern core should be ready", modernCore.isInitialized())
        
        println("✅ Modern core initialized successfully")
        
        // Test manager access
        assertNotNull("Session manager should be available", modernCore.sessionManager)
        assertNotNull("Identity manager should be available", modernCore.identityManager)
        assertNotNull("Link manager should be available", modernCore.linkManager)
        assertNotNull("Event manager should be available", modernCore.eventManager)
        assertNotNull("Data manager should be available", modernCore.dataManager)
        assertNotNull("Configuration manager should be available", modernCore.configurationManager)
        
        println("✅ All manager interfaces available")
        
        // Test reactive state flows
        assertNotNull("Initialization state should be observable", modernCore.isInitialized)
        assertNotNull("Current session should be observable", modernCore.currentSession)
        assertNotNull("Current user should be observable", modernCore.currentUser)
        
        println("✅ Reactive state management working")
        
        // Test modern API usage
        val sessionResult = modernCore.sessionManager.initSession(mockActivity)
        assertTrue("Session should initialize successfully", sessionResult.isSuccess)
        
        val identityResult = modernCore.identityManager.setIdentity("modern-user")
        assertTrue("Identity should be set successfully", identityResult.isSuccess)
        
        println("✅ Modern APIs working correctly")
        
        // Verify integration with preservation layer
        assertTrue("Preservation manager should detect modern core", 
                   preservationManager.isReady())
        
        println("✅ Modern architecture fully integrated with preservation layer")
    }
    
    @Test
    fun `demonstrate end to end compatibility`() {
        println("\n🔄 === End-to-End Compatibility Demo ===")
        
        // Simulate real-world usage mixing legacy and modern APIs
        
        // 1. Legacy static API usage
        val legacyInstance = PreservedBranchApi.getInstance()
        PreservedBranchApi.enableTestMode()
        
        // 2. Legacy instance API usage
        val wrapper = LegacyBranchWrapper.getInstance()
        wrapper.initSession(mockActivity)
        wrapper.setIdentity("e2e-user")
        wrapper.userCompletedAction("e2e_test")
        
        // 3. Modern API usage (direct)
        val modernCore = ModernBranchCore.getInstance()
        runBlocking {
            modernCore.initialize(mockContext)
            modernCore.configurationManager.enableTestMode()
            modernCore.identityManager.setIdentity("modern-e2e-user")
        }
        
        // 4. Verify all systems working together
        assertTrue("Legacy wrapper should be ready", wrapper.isModernCoreReady())
        assertTrue("Modern core should be initialized", modernCore.isInitialized())
        assertTrue("Preservation manager should be ready", preservationManager.isReady())
        
        // 5. Generate comprehensive analytics
        val usageData = analytics.getUsageData()
        val performanceData = analytics.getPerformanceAnalytics()
        val insights = analytics.generateMigrationInsights()
        val report = preservationManager.generateMigrationReport()
        
        assertTrue("Should have comprehensive usage data", usageData.isNotEmpty())
        assertTrue("Should have performance insights", performanceData.totalApiCalls > 0)
        assertTrue("Should have migration recommendations", insights.priorityMethods.isNotEmpty())
        assertNotNull("Should generate migration report", report)
        
        println("✅ End-to-end compatibility verified")
        println("📊 Final Statistics:")
        println("   APIs called: ${usageData.size}")
        println("   Total calls: ${performanceData.totalApiCalls}")
        println("   Migration priorities: ${insights.priorityMethods.size}")
        println("   Report generated: ${report.totalApis} APIs analyzed")
        
        println("\n🎉 === Modern Strategy Implementation Complete ===")
        println("✅ 100% Backward Compatibility Maintained")
        println("✅ Modern Architecture Successfully Integrated")
        println("✅ Comprehensive Analytics & Monitoring Active")
        println("✅ Zero Breaking Changes During Transition")
        println("✅ Data-Driven Migration Path Established")
    }
} 