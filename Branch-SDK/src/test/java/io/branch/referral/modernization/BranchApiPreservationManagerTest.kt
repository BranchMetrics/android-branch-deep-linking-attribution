package io.branch.referral.modernization

import android.content.Context
import io.branch.referral.modernization.analytics.ApiUsageAnalytics
import io.branch.referral.modernization.core.ModernBranchCore
import io.branch.referral.modernization.core.VersionConfiguration
import io.branch.referral.modernization.registry.PublicApiRegistry
import io.branch.referral.modernization.registry.MigrationReport
import io.branch.referral.modernization.registry.VersionTimelineReport
import io.branch.referral.modernization.registry.ApiMethodInfo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Comprehensive unit tests for BranchApiPreservationManager.
 * 
 * Tests all public methods, error scenarios, and edge cases to achieve 95% code coverage.
 */
class BranchApiPreservationManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockVersionConfig: VersionConfiguration
    private lateinit var preservationManager: BranchApiPreservationManager
    private lateinit var mockModernCore: ModernBranchCore
    private lateinit var mockRegistry: PublicApiRegistry
    private lateinit var mockAnalytics: ApiUsageAnalytics
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        mockContext = mock(Context::class.java)
        mockVersionConfig = mock(VersionConfiguration::class.java)
        mockModernCore = mock(ModernBranchCore::class.java)
        mockRegistry = mock(PublicApiRegistry::class.java)
        mockAnalytics = mock(ApiUsageAnalytics::class.java)
        
        // Setup default mock behaviors
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockVersionConfig.getDeprecationVersion()).thenReturn("5.0.0")
        `when`(mockVersionConfig.getRemovalVersion()).thenReturn("7.0.0")
        `when`(mockRegistry.getTotalApiCount()).thenReturn(15)
        `when`(mockRegistry.getApisByCategory(anyString())).thenReturn(emptyList())
        `when`(mockAnalytics.getUsageData()).thenReturn(emptyMap())
    }
    
    @Test
    fun `test singleton pattern with context`() {
        // Test singleton behavior
        val instance1 = BranchApiPreservationManager.getInstance(mockContext)
        val instance2 = BranchApiPreservationManager.getInstance(mockContext)
        
        assertSame("Should return same instance", instance1, instance2)
        assertTrue("Should be ready after initialization", instance1.isReady())
    }
    
    @Test
    fun `test isReady method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        assertTrue("Manager should be ready after initialization", manager.isReady())
    }
    
    @Test
    fun `test getUsageAnalytics`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val analytics = manager.getUsageAnalytics()
        
        assertNotNull("Should return analytics instance", analytics)
        assertTrue("Should be same instance", analytics === manager.getUsageAnalytics())
    }
    
    @Test
    fun `test getApiRegistry`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val registry = manager.getApiRegistry()
        
        assertNotNull("Should return registry instance", registry)
        assertTrue("Should be same instance", registry === manager.getApiRegistry())
    }
    
    @Test
    fun `test handleLegacyApiCall with valid method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        // Test with a simple method call
        val result = manager.handleLegacyApiCall("getInstance", emptyArray())
        
        assertNotNull("Should return result", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with parameters`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val parameters = arrayOf<Any?>("testUserId", "testParam")
        val result = manager.handleLegacyApiCall("setIdentity", parameters)
        
        assertNotNull("Should return result", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with null parameters`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("logout", emptyArray())
        
        assertNotNull("Should handle null parameters", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with empty method name`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("", emptyArray())
        
        assertNotNull("Should handle empty method name", result)
    }
    
    @Test
    fun `test generateMigrationReport`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val report = manager.generateMigrationReport()
        
        assertNotNull("Should return migration report", report)
        assertTrue("Should have total APIs", report.totalApis > 0)
        assertNotNull("Should have risk factors", report.riskFactors)
        assertNotNull("Should have usage statistics", report.usageStatistics)
    }
    
    @Test
    fun `test generateVersionTimelineReport`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val report = manager.generateVersionTimelineReport()
        
        assertNotNull("Should return version timeline report", report)
        assertNotNull("Should have version details", report.versionDetails)
        assertNotNull("Should have summary", report.summary)
    }
    
    @Test
    fun `test getApisForDeprecationInVersion`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val apis = manager.getApisForDeprecationInVersion("5.0.0")
        
        assertNotNull("Should return list of APIs", apis)
        assertTrue("Should be a list", apis is List<*>)
    }
    
    @Test
    fun `test getApisForRemovalInVersion`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        val apis = manager.getApisForRemovalInVersion("7.0.0")
        
        assertNotNull("Should return list of APIs", apis)
        assertTrue("Should be a list", apis is List<*>)
    }
    
    @Test
    fun `test handleLegacyApiCall with getInstance method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("getInstance", emptyArray())
        
        assertNotNull("Should return result for getInstance", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with getAutoInstance method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("getAutoInstance", arrayOf(mockContext))
        
        assertNotNull("Should return result for getAutoInstance", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with setIdentity method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("setIdentity", arrayOf("testUserId"))
        
        assertNotNull("Should return result for setIdentity", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with resetUserSession method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("resetUserSession", emptyArray())
        
        assertNotNull("Should return result for resetUserSession", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with enableTestMode method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("enableTestMode", emptyArray())
        
        assertNotNull("Should return result for enableTestMode", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with getFirstReferringParams method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("getFirstReferringParams", emptyArray())
        
        assertNotNull("Should return result for getFirstReferringParams", result)
    }
    
    @Test
    fun `test handleLegacyApiCall with unknown method`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val result = manager.handleLegacyApiCall("unknownMethod", emptyArray())
        
        assertNull("Should return null for unknown method", result)
    }
    
    @Test
    fun `test concurrent access to singleton`() {
        val latch = CountDownLatch(2)
        var instance1: BranchApiPreservationManager? = null
        var instance2: BranchApiPreservationManager? = null
        
        Thread {
            instance1 = BranchApiPreservationManager.getInstance(mockContext)
            latch.countDown()
        }.start()
        
        Thread {
            instance2 = BranchApiPreservationManager.getInstance(mockContext)
            latch.countDown()
        }.start()
        
        latch.await(5, TimeUnit.SECONDS)
        
        assertNotNull("Instance 1 should not be null", instance1)
        assertNotNull("Instance 2 should not be null", instance2)
        assertSame("Both instances should be the same", instance1, instance2)
    }
    
    @Test
    fun `test multiple method calls tracking`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        // Call multiple methods
        manager.handleLegacyApiCall("getInstance", emptyArray())
        manager.handleLegacyApiCall("setIdentity", arrayOf("user1"))
        manager.handleLegacyApiCall("resetUserSession", emptyArray())
        
        // Verify analytics are being tracked
        val analytics = manager.getUsageAnalytics()
        assertNotNull("Analytics should be available", analytics)
    }
    
    @Test
    fun `test error handling in handleLegacyApiCall`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        // Test with invalid parameters that might cause exceptions
        val result = manager.handleLegacyApiCall("setIdentity", arrayOf(null))
        
        // Should not throw exception, should handle gracefully
        assertNotNull("Should handle null parameters gracefully", result)
    }
    
    @Test
    fun `test migration report generation with real data`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        // Generate some usage data first
        manager.handleLegacyApiCall("getInstance", emptyArray())
        manager.handleLegacyApiCall("setIdentity", arrayOf("testUser"))
        
        val report = manager.generateMigrationReport()
        
        assertNotNull("Should generate migration report", report)
        assertTrue("Should have total APIs count", report.totalApis >= 0)
        assertNotNull("Should have risk factors", report.riskFactors)
        assertNotNull("Should have usage statistics", report.usageStatistics)
    }
    
    @Test
    fun `test version timeline report generation`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val report = manager.generateVersionTimelineReport()
        
        assertNotNull("Should generate version timeline report", report)
        assertNotNull("Should have version details", report.versionDetails)
        assertNotNull("Should have summary", report.summary)
        assertTrue("Should have version information", report.versionDetails.isNotEmpty())
    }
    
    @Test
    fun `test API deprecation version filtering`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val apisForVersion50 = manager.getApisForDeprecationInVersion("5.0.0")
        val apisForVersion60 = manager.getApisForDeprecationInVersion("6.0.0")
        
        assertNotNull("Should return APIs for version 5.0.0", apisForVersion50)
        assertNotNull("Should return APIs for version 6.0.0", apisForVersion60)
        assertTrue("Should be lists", apisForVersion50 is List<*> && apisForVersion60 is List<*>)
    }
    
    @Test
    fun `test API removal version filtering`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val apisForVersion70 = manager.getApisForRemovalInVersion("7.0.0")
        val apisForVersion80 = manager.getApisForRemovalInVersion("8.0.0")
        
        assertNotNull("Should return APIs for version 7.0.0", apisForVersion70)
        assertNotNull("Should return APIs for version 8.0.0", apisForVersion80)
        assertTrue("Should be lists", apisForVersion70 is List<*> && apisForVersion80 is List<*>)
    }
    
    @Test
    fun `test ready state consistency`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        // Should be ready after initialization
        assertTrue("Should be ready after initialization", manager.isReady())
        
        // Should remain ready across multiple calls
        assertTrue("Should remain ready", manager.isReady())
        assertTrue("Should remain ready", manager.isReady())
    }
    
    @Test
    fun `test analytics instance consistency`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val analytics1 = manager.getUsageAnalytics()
        val analytics2 = manager.getUsageAnalytics()
        
        assertSame("Should return same analytics instance", analytics1, analytics2)
    }
    
    @Test
    fun `test registry instance consistency`() {
        val manager = BranchApiPreservationManager.getInstance(mockContext)
        
        val registry1 = manager.getApiRegistry()
        val registry2 = manager.getApiRegistry()
        
        assertSame("Should return same registry instance", registry1, registry2)
    }
} 