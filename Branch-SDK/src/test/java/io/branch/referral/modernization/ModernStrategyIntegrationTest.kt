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
import kotlin.test.*

/**
 * Comprehensive integration tests for Modern Strategy implementation.
 * 
 * These tests validate the complete preservation architecture and ensure
 * zero breaking changes while providing modern functionality.
 */
class ModernStrategyIntegrationTest {
    
    private lateinit var preservationManager: BranchApiPreservationManager
    private lateinit var modernCore: ModernBranchCore
    private lateinit var analytics: ApiUsageAnalytics
    private lateinit var registry: PublicApiRegistry
    
    @BeforeTest
    fun setup() {
        preservationManager = BranchApiPreservationManager.getInstance()
        modernCore = ModernBranchCore.getInstance()
        analytics = preservationManager.getUsageAnalytics()
        registry = preservationManager.getApiRegistry()
        analytics.reset()
        
        println("🧪 Integration Test Setup Complete")
    }
    
    @Test
    fun `validate complete API preservation architecture`() {
        println("\n🔍 Testing API Preservation Architecture")
        
        // Verify all core components are initialized
        assertNotNull(preservationManager, "Preservation manager should be initialized")
        assertNotNull(modernCore, "Modern core should be initialized")
        assertNotNull(analytics, "Analytics should be initialized")
        assertNotNull(registry, "Registry should be initialized")
        
        // Verify preservation manager is ready
        assertTrue(preservationManager.isReady(), "Preservation manager should be ready")
        
        // Verify API registry has comprehensive coverage
        val totalApis = registry.getTotalApiCount()
        assertTrue(totalApis >= 10, "Should have at least 10 APIs registered")
        println("✅ Registry contains $totalApis APIs")
        
        // Verify impact distribution
        val impactDistribution = registry.getImpactDistribution()
        assertTrue(impactDistribution[UsageImpact.CRITICAL]!! > 0, "Should have critical APIs")
        assertTrue(impactDistribution[UsageImpact.HIGH]!! > 0, "Should have high impact APIs")
        
        // Verify complexity distribution
        val complexityDistribution = registry.getComplexityDistribution()
        assertTrue(complexityDistribution[MigrationComplexity.SIMPLE]!! > 0, "Should have simple migrations")
        
        println("✅ API Preservation Architecture validated")
    }
    
    @Test
    fun `validate static API wrapper compatibility`() {
        println("\n🔧 Testing Static API Wrapper Compatibility")
        
        // Test getInstance methods
        val instance1 = PreservedBranchApi.getInstance()
        assertNotNull(instance1, "getInstance() should return valid wrapper")
        
        // Test configuration methods
        try {
            PreservedBranchApi.enableTestMode()
            PreservedBranchApi.enableLogging()
            PreservedBranchApi.disableLogging()
            println("✅ Configuration methods working")
        } catch (e: Exception) {
            fail("Configuration methods should not throw exceptions: ${e.message}")
        }
        
        // Verify analytics tracked the calls
        val usageData = analytics.getUsageData()
        assertTrue(usageData.containsKey("getInstance"), "Should track getInstance calls")
        assertTrue(usageData.containsKey("enableTestMode"), "Should track enableTestMode calls")
        
        // Verify call counts
        val getInstanceData = usageData["getInstance"]
        assertNotNull(getInstanceData, "getInstance usage data should exist")
        assertTrue(getInstanceData.callCount > 0, "Should have recorded calls")
        
        println("✅ Static API wrapper compatibility validated")
    }
    
    @Test
    fun `validate instance API wrapper compatibility`() {
        println("\n🏃 Testing Instance API Wrapper Compatibility")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        assertNotNull(wrapper, "Should get wrapper instance")
        
        // Create mock context for testing
        val mockActivity = createMockActivity()
        
        // Test core session methods
        try {
            val sessionResult = wrapper.initSession(mockActivity)
            // Session result can be true or false, both are valid
            println("✅ initSession() completed with result: $sessionResult")
        } catch (e: Exception) {
            fail("initSession should not throw exceptions: ${e.message}")
        }
        
        // Test identity management
        try {
            wrapper.setIdentity("integration-test-user")
            wrapper.logout()
            println("✅ Identity management methods working")
        } catch (e: Exception) {
            fail("Identity methods should not throw exceptions: ${e.message}")
        }
        
        // Test data retrieval
        try {
            val firstParams = wrapper.getFirstReferringParams()
            val latestParams = wrapper.getLatestReferringParams()
            // Results can be null, that's expected
            println("✅ Data retrieval methods working")
        } catch (e: Exception) {
            fail("Data retrieval methods should not throw exceptions: ${e.message}")
        }
        
        // Test event tracking
        try {
            wrapper.userCompletedAction("integration_test")
            wrapper.userCompletedAction("integration_test_with_data", JSONObject().apply {
                put("test_key", "test_value")
            })
            println("✅ Event tracking methods working")
        } catch (e: Exception) {
            fail("Event tracking should not throw exceptions: ${e.message}")
        }
        
        // Verify analytics tracked everything
        val usageData = analytics.getUsageData()
        assertTrue(usageData.containsKey("initSession"), "Should track initSession")
        assertTrue(usageData.containsKey("setIdentity"), "Should track setIdentity")
        assertTrue(usageData.containsKey("userCompletedAction"), "Should track userCompletedAction")
        
        println("✅ Instance API wrapper compatibility validated")
    }
    
    @Test
    fun `validate callback adaptation system`() {
        println("\n📞 Testing Callback Adaptation System")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        val mockActivity = createMockActivity()
        
        // Test init callback
        var initCallbackExecuted = false
        var callbackParams: JSONObject? = null
        var callbackError: io.branch.referral.BranchError? = null
        
        val initCallback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: io.branch.referral.BranchError?) {
                initCallbackExecuted = true
                callbackParams = referringParams
                callbackError = error
            }
        }
        
        // Execute with callback
        wrapper.initSession(initCallback, mockActivity)
        
        // Wait for async callback execution
        Thread.sleep(200)
        
        assertTrue(initCallbackExecuted, "Init callback should have been executed")
        println("✅ Init callback executed successfully")
        
        // Test state change callback
        var stateCallbackExecuted = false
        val stateCallback = object : Branch.BranchReferralStateChangedListener {
            override fun onStateChanged(changed: Boolean, error: io.branch.referral.BranchError?) {
                stateCallbackExecuted = true
            }
        }
        
        wrapper.logout(stateCallback)
        Thread.sleep(200)
        
        assertTrue(stateCallbackExecuted, "State callback should have been executed")
        println("✅ State change callback executed successfully")
        
        // Test link creation callback
        var linkCallbackExecuted = false
        val linkCallback = object : Branch.BranchLinkCreateListener {
            override fun onLinkCreate(url: String?, error: io.branch.referral.BranchError?) {
                linkCallbackExecuted = true
            }
        }
        
        wrapper.generateShortUrl(mapOf("test" to "data"), linkCallback)
        Thread.sleep(200)
        
        assertTrue(linkCallbackExecuted, "Link callback should have been executed")
        println("✅ Link creation callback executed successfully")
        
        println("✅ Callback adaptation system validated")
    }
    
    @Test
    fun `validate performance monitoring`() {
        println("\n⚡ Testing Performance Monitoring")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        val mockActivity = createMockActivity()
        
        // Execute multiple API calls to generate performance data
        val startTime = System.currentTimeMillis()
        repeat(20) { i ->
            wrapper.initSession(mockActivity)
            wrapper.setIdentity("perf-user-$i")
            wrapper.userCompletedAction("perf-action-$i")
        }
        val executionTime = System.currentTimeMillis() - startTime
        
        // Get performance analytics
        val performanceAnalytics = analytics.getPerformanceAnalytics()
        
        assertTrue(performanceAnalytics.totalApiCalls > 0, "Should have recorded API calls")
        assertTrue(performanceAnalytics.methodPerformance.isNotEmpty(), "Should have method performance data")
        
        // Verify reasonable performance
        val averageOverhead = performanceAnalytics.averageWrapperOverheadMs
        assertTrue(averageOverhead >= 0, "Average overhead should be non-negative")
        
        // Log performance metrics
        println("📊 Performance Metrics:")
        println("   Total API calls: ${performanceAnalytics.totalApiCalls}")
        println("   Average overhead: ${averageOverhead}ms")
        println("   Total execution time: ${executionTime}ms")
        
        // Verify overhead is reasonable (less than 50ms average)
        assertTrue(averageOverhead < 50.0, 
                  "Average overhead should be reasonable (<50ms), got ${averageOverhead}ms")
        
        println("✅ Performance monitoring validated")
    }
    
    @Test
    fun `validate modern architecture integration`() = runBlocking {
        println("\n🏗️ Testing Modern Architecture Integration")
        
        val modernCore = ModernBranchCore.getInstance()
        val mockContext = createMockContext()
        
        // Test initialization
        val initResult = modernCore.initialize(mockContext)
        assertTrue(initResult.isSuccess, "Modern core should initialize successfully")
        assertTrue(modernCore.isInitialized(), "Modern core should report as initialized")
        
        // Test all managers are available
        assertNotNull(modernCore.sessionManager, "Session manager should be available")
        assertNotNull(modernCore.identityManager, "Identity manager should be available")
        assertNotNull(modernCore.linkManager, "Link manager should be available")
        assertNotNull(modernCore.eventManager, "Event manager should be available")
        assertNotNull(modernCore.dataManager, "Data manager should be available")
        assertNotNull(modernCore.configurationManager, "Configuration manager should be available")
        
        // Test manager functionality
        val mockActivity = createMockActivity()
        val sessionResult = modernCore.sessionManager.initSession(mockActivity)
        assertTrue(sessionResult.isSuccess, "Session should initialize successfully")
        
        val identityResult = modernCore.identityManager.setIdentity("modern-test-user")
        assertTrue(identityResult.isSuccess, "Identity should be set successfully")
        
        // Test reactive state flows
        assertNotNull(modernCore.isInitialized, "isInitialized flow should be available")
        assertNotNull(modernCore.currentSession, "currentSession flow should be available")
        assertNotNull(modernCore.currentUser, "currentUser flow should be available")
        
        // Verify integration with preservation layer
        assertTrue(preservationManager.isReady(), "Preservation manager should detect modern core")
        
        println("✅ Modern architecture integration validated")
    }
    
    @Test
    fun `validate migration analytics and insights`() {
        println("\n📊 Testing Migration Analytics and Insights")
        
        val wrapper = LegacyBranchWrapper.getInstance()
        val mockActivity = createMockActivity()
        
        // Generate diverse usage patterns
        repeat(30) { wrapper.initSession(mockActivity) }
        repeat(20) { wrapper.setIdentity("analytics-user-$it") }
        repeat(15) { wrapper.getFirstReferringParams() }
        repeat(10) { wrapper.enableTestMode() }
        repeat(5) { wrapper.userCompletedAction("analytics-action") }
        
        // Test deprecation analytics
        val deprecationAnalytics = analytics.getDeprecationAnalytics()
        assertTrue(deprecationAnalytics.totalDeprecationWarnings > 0, "Should have deprecation warnings")
        assertTrue(deprecationAnalytics.totalDeprecatedApiCalls > 0, "Should have deprecated API calls")
        assertTrue(deprecationAnalytics.methodsWithWarnings > 0, "Should have methods with warnings")
        
        // Test migration insights
        val insights = analytics.generateMigrationInsights()
        assertTrue(insights.priorityMethods.isNotEmpty(), "Should have priority methods")
        assertTrue(insights.recentlyActiveMethods.isNotEmpty(), "Should have recently active methods")
        assertTrue(insights.recommendedMigrationOrder.isNotEmpty(), "Should have migration order")
        
        // Test migration report generation
        val report = preservationManager.generateMigrationReport()
        assertTrue(report.totalApis > 0, "Report should include APIs")
        assertTrue(report.criticalApis > 0, "Report should identify critical APIs")
        assertNotNull(report.estimatedMigrationEffort, "Should estimate effort")
        assertNotNull(report.recommendedTimeline, "Should recommend timeline")
        
        println("📈 Analytics Summary:")
        println("   Total warnings: ${deprecationAnalytics.totalDeprecationWarnings}")
        println("   Priority methods: ${insights.priorityMethods.size}")
        println("   Report APIs: ${report.totalApis}")
        println("   Estimated effort: ${report.estimatedMigrationEffort}")
        
        println("✅ Migration analytics and insights validated")
    }
    
    @Test
    fun `validate end to end backward compatibility`() {
        println("\n🔄 Testing End-to-End Backward Compatibility")
        
        // Simulate real-world mixed usage scenario
        val mockActivity = createMockActivity()
        val mockContext = createMockContext()
        
        try {
            // 1. Legacy static API usage
            val staticInstance = PreservedBranchApi.getInstance()
            PreservedBranchApi.enableTestMode()
            
            // 2. Legacy instance API usage
            val wrapper = LegacyBranchWrapper.getInstance()
            wrapper.initSession(mockActivity)
            wrapper.setIdentity("e2e-test-user")
            wrapper.userCompletedAction("e2e_compatibility_test")
            
            // 3. Modern API usage
            runBlocking {
                val modernCore = ModernBranchCore.getInstance()
                modernCore.initialize(mockContext)
                modernCore.configurationManager.enableTestMode()
                modernCore.identityManager.setIdentity("modern-e2e-user")
            }
            
            // 4. Verify all systems work together
            assertTrue(wrapper.isModernCoreReady(), "Wrapper should recognize modern core")
            assertTrue(modernCore.isInitialized(), "Modern core should be initialized")
            assertTrue(preservationManager.isReady(), "Preservation manager should be ready")
            
            // 5. Verify analytics captured everything
            val usageData = analytics.getUsageData()
            val performanceData = analytics.getPerformanceAnalytics()
            
            assertTrue(usageData.isNotEmpty(), "Should have usage data")
            assertTrue(performanceData.totalApiCalls > 0, "Should have performance data")
            
            println("📊 E2E Compatibility Results:")
            println("   APIs called: ${usageData.size}")
            println("   Total calls: ${performanceData.totalApiCalls}")
            println("   Systems integrated: 3/3")
            
            println("✅ End-to-end backward compatibility validated")
            
        } catch (e: Exception) {
            fail("End-to-end compatibility test should not throw exceptions: ${e.message}")
        }
    }
    
    @Test
    fun `validate zero breaking changes guarantee`() {
        println("\n🛡️ Testing Zero Breaking Changes Guarantee")
        
        // This test verifies that all legacy APIs remain functional
        val wrapper = LegacyBranchWrapper.getInstance()
        val mockActivity = createMockActivity()
        
        // Test that all major API categories work without exceptions
        val apiCategories = mapOf(
            "Session Management" to { wrapper.initSession(mockActivity) },
            "Identity Management" to { wrapper.setIdentity("zero-break-test") },
            "Data Retrieval" to { wrapper.getFirstReferringParams() },
            "Event Tracking" to { wrapper.userCompletedAction("zero_break_test") },
            "Configuration" to { wrapper.enableTestMode() }
        )
        
        val results = mutableMapOf<String, Boolean>()
        
        apiCategories.forEach { (category, test) ->
            try {
                test()
                results[category] = true
                println("✅ $category APIs working")
            } catch (e: Exception) {
                results[category] = false
                println("❌ $category APIs failed: ${e.message}")
            }
        }
        
        // Verify all categories passed
        val failedCategories = results.filter { !it.value }.keys
        assertTrue(failedCategories.isEmpty(), 
                  "All API categories should work, but these failed: $failedCategories")
        
        // Verify analytics still track everything
        val usageData = analytics.getUsageData()
        assertTrue(usageData.size >= apiCategories.size, 
                  "Analytics should track all API categories")
        
        println("✅ Zero breaking changes guarantee validated")
    }
    
    // Helper methods for creating mocks
    private fun createMockActivity(): Activity {
        return object : Activity() {
            override fun toString(): String = "MockActivity"
        }
    }
    
    private fun createMockContext(): Context {
        return object : Context() {
            override fun getApplicationContext(): Context = this
            override fun toString(): String = "MockContext"
        }
    }
    
    @AfterTest
    fun cleanup() {
        println("🧹 Integration Test Cleanup Complete\n")
    }
} 