package io.branch.referral.modernization.registry

import io.branch.referral.modernization.core.VersionConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * Comprehensive unit tests for PublicApiRegistry.
 * 
 * Tests all public methods, API registration, and report generation to achieve 95% code coverage.
 */
class PublicApiRegistryTest {
    
    private lateinit var mockVersionConfig: VersionConfiguration
    private lateinit var registry: PublicApiRegistry
    
    @Before
    fun setup() {
        mockVersionConfig = mock(VersionConfiguration::class.java)
        `when`(mockVersionConfig.getDeprecationVersion()).thenReturn("5.0.0")
        `when`(mockVersionConfig.getRemovalVersion()).thenReturn("7.0.0")
        
        registry = PublicApiRegistry(mockVersionConfig)
    }
    
    @Test
    fun `test registerApi`() {
        registry.registerApi(
            methodName = "testMethod",
            signature = "testMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod()"
        )
        
        assertEquals("Should have 1 API registered", 1, registry.getTotalApiCount())
        
        val apiInfo = registry.getApiInfo("testMethod")
        assertNotNull("Should return API info", apiInfo)
        assertEquals("Should have correct method name", "testMethod", apiInfo.methodName)
        assertEquals("Should have correct signature", "testMethod()", apiInfo.signature)
        assertEquals("Should have correct usage impact", UsageImpact.MEDIUM, apiInfo.usageImpact)
        assertEquals("Should have correct complexity", MigrationComplexity.SIMPLE, apiInfo.migrationComplexity)
    }
    
    @Test
    fun `test registerApi with all parameters`() {
        registry.registerApi(
            methodName = "fullMethod",
            signature = "fullMethod(String, int)",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "modernMethod()",
            category = "Custom Category",
            breakingChanges = listOf("Parameter order changed"),
            migrationNotes = "Requires careful migration",
            deprecationVersion = "4.5.0",
            removalVersion = "6.5.0"
        )
        
        val apiInfo = registry.getApiInfo("fullMethod")
        assertNotNull("Should return API info", apiInfo)
        assertEquals("Should have correct category", "Custom Category", apiInfo.category)
        assertEquals("Should have breaking changes", listOf("Parameter order changed"), apiInfo.breakingChanges)
        assertEquals("Should have migration notes", "Requires careful migration", apiInfo.migrationNotes)
        assertEquals("Should have custom deprecation version", "4.5.0", apiInfo.deprecationVersion)
        assertEquals("Should have custom removal version", "6.5.0", apiInfo.removalVersion)
    }
    
    @Test
    fun `test getApiInfo for non-existent method`() {
        val apiInfo = registry.getApiInfo("nonExistentMethod")
        assertNull("Should return null for non-existent method", apiInfo)
    }
    
    @Test
    fun `test getApisByCategory`() {
        registry.registerApi(
            methodName = "sessionMethod",
            signature = "sessionMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q3 2025",
            modernReplacement = "sessionManager.method()",
            category = "Session Management"
        )
        
        registry.registerApi(
            methodName = "identityMethod",
            signature = "identityMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q3 2025",
            modernReplacement = "identityManager.method()",
            category = "Identity Management"
        )
        
        val sessionApis = registry.getApisByCategory("Session Management")
        assertEquals("Should have 1 session API", 1, sessionApis.size)
        assertEquals("Should have correct method", "sessionMethod", sessionApis[0].methodName)
        
        val identityApis = registry.getApisByCategory("Identity Management")
        assertEquals("Should have 1 identity API", 1, identityApis.size)
        assertEquals("Should have correct method", "identityMethod", identityApis[0].methodName)
    }
    
    @Test
    fun `test getApisByCategory for non-existent category`() {
        val apis = registry.getApisByCategory("NonExistentCategory")
        assertTrue("Should return empty list", apis.isEmpty())
    }
    
    @Test
    fun `test getApisByImpact`() {
        registry.registerApi(
            methodName = "criticalMethod",
            signature = "criticalMethod()",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "criticalManager.method()"
        )
        
        registry.registerApi(
            methodName = "mediumMethod",
            signature = "mediumMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "mediumManager.method()"
        )
        
        val criticalApis = registry.getApisByImpact(UsageImpact.CRITICAL)
        assertEquals("Should have 1 critical API", 1, criticalApis.size)
        assertEquals("Should have correct method", "criticalMethod", criticalApis[0].methodName)
        
        val mediumApis = registry.getApisByImpact(UsageImpact.MEDIUM)
        assertEquals("Should have 1 medium API", 1, mediumApis.size)
        assertEquals("Should have correct method", "mediumMethod", mediumApis[0].methodName)
    }
    
    @Test
    fun `test getApisByComplexity`() {
        registry.registerApi(
            methodName = "simpleMethod",
            signature = "simpleMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "simpleManager.method()"
        )
        
        registry.registerApi(
            methodName = "complexMethod",
            signature = "complexMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "complexManager.method()"
        )
        
        val simpleApis = registry.getApisByComplexity(MigrationComplexity.SIMPLE)
        assertEquals("Should have 1 simple API", 1, simpleApis.size)
        assertEquals("Should have correct method", "simpleMethod", simpleApis[0].methodName)
        
        val complexApis = registry.getApisByComplexity(MigrationComplexity.COMPLEX)
        assertEquals("Should have 1 complex API", 1, complexApis.size)
        assertEquals("Should have correct method", "complexMethod", complexApis[0].methodName)
    }
    
    @Test
    fun `test getTotalApiCount`() {
        assertEquals("Should start with 0 APIs", 0, registry.getTotalApiCount())
        
        registry.registerApi(
            methodName = "method1",
            signature = "method1()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "newMethod1()"
        )
        
        assertEquals("Should have 1 API", 1, registry.getTotalApiCount())
        
        registry.registerApi(
            methodName = "method2",
            signature = "method2()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod2()"
        )
        
        assertEquals("Should have 2 APIs", 2, registry.getTotalApiCount())
    }
    
    @Test
    fun `test getAllCategories`() {
        registry.registerApi(
            methodName = "sessionMethod",
            signature = "sessionMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q3 2025",
            modernReplacement = "sessionManager.method()",
            category = "Session Management"
        )
        
        registry.registerApi(
            methodName = "identityMethod",
            signature = "identityMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q3 2025",
            modernReplacement = "identityManager.method()",
            category = "Identity Management"
        )
        
        val categories = registry.getAllCategories()
        assertEquals("Should have 2 categories", 2, categories.size)
        assertTrue("Should contain Session Management", categories.contains("Session Management"))
        assertTrue("Should contain Identity Management", categories.contains("Identity Management"))
    }
    
    @Test
    fun `test generateVersionTimelineReport`() {
        registry.registerApi(
            methodName = "deprecatedMethod",
            signature = "deprecatedMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod()",
            deprecationVersion = "4.5.0",
            removalVersion = "5.5.0"
        )
        
        registry.registerApi(
            methodName = "removedMethod",
            signature = "removedMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "newMethod()",
            deprecationVersion = "4.0.0",
            removalVersion = "5.0.0"
        )
        
        val report = registry.generateVersionTimelineReport()
        
        assertNotNull("Should return timeline report", report)
        assertTrue("Should have version details", report.versionDetails.isNotEmpty())
        assertNotNull("Should have summary", report.summary)
        assertTrue("Should have total versions", report.totalVersions > 0)
    }
    
    @Test
    fun `test generateMigrationReport`() {
        registry.registerApi(
            methodName = "criticalMethod",
            signature = "criticalMethod()",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "criticalManager.method()"
        )
        
        registry.registerApi(
            methodName = "simpleMethod",
            signature = "simpleMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "simpleManager.method()"
        )
        
        val usageData = mapOf(
            "criticalMethod" to ApiUsageData(
                methodName = "criticalMethod",
                callCount = 1000,
                lastUsed = System.currentTimeMillis(),
                averageCallsPerDay = 50.0,
                uniqueApplications = 1
            ),
            "simpleMethod" to ApiUsageData(
                methodName = "simpleMethod",
                callCount = 100,
                lastUsed = System.currentTimeMillis(),
                averageCallsPerDay = 5.0,
                uniqueApplications = 1
            )
        )
        
        val report = registry.generateMigrationReport(usageData)
        
        assertNotNull("Should return migration report", report)
        assertEquals("Should have correct total APIs", 2, report.totalApis)
        assertTrue("Should have risk factors", report.riskFactors.isNotEmpty())
        assertTrue("Should have recommendations", report.recommendations.isNotEmpty())
        assertTrue("Should have migration timeline", report.migrationTimeline.isNotEmpty())
    }
    
    @Test
    fun `test getImpactDistribution`() {
        registry.registerApi(
            methodName = "criticalMethod",
            signature = "criticalMethod()",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "criticalManager.method()"
        )
        
        registry.registerApi(
            methodName = "highMethod",
            signature = "highMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q3 2025",
            modernReplacement = "highManager.method()"
        )
        
        registry.registerApi(
            methodName = "mediumMethod",
            signature = "mediumMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "mediumManager.method()"
        )
        
        val distribution = registry.getImpactDistribution()
        
        assertEquals("Should have 1 critical API", 1, distribution[UsageImpact.CRITICAL])
        assertEquals("Should have 1 high API", 1, distribution[UsageImpact.HIGH])
        assertEquals("Should have 1 medium API", 1, distribution[UsageImpact.MEDIUM])
        assertEquals("Should have 0 low APIs", 0, distribution[UsageImpact.LOW])
    }
    
    @Test
    fun `test getComplexityDistribution`() {
        registry.registerApi(
            methodName = "simpleMethod",
            signature = "simpleMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "simpleManager.method()"
        )
        
        registry.registerApi(
            methodName = "mediumMethod",
            signature = "mediumMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q2 2025",
            modernReplacement = "mediumManager.method()"
        )
        
        registry.registerApi(
            methodName = "complexMethod",
            signature = "complexMethod()",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q4 2025",
            modernReplacement = "complexManager.method()"
        )
        
        val distribution = registry.getComplexityDistribution()
        
        assertEquals("Should have 1 simple API", 1, distribution[MigrationComplexity.SIMPLE])
        assertEquals("Should have 1 medium API", 1, distribution[MigrationComplexity.MEDIUM])
        assertEquals("Should have 1 complex API", 1, distribution[MigrationComplexity.COMPLEX])
    }
    
    @Test
    fun `test category inference`() {
        // Test that category is inferred from signature when not provided
        registry.registerApi(
            methodName = "initSession",
            signature = "Branch.initSession(Activity)",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q3 2025",
            modernReplacement = "sessionManager.initSession()"
        )
        
        val apiInfo = registry.getApiInfo("initSession")
        assertNotNull("Should return API info", apiInfo)
        assertTrue("Should have inferred category", apiInfo.category.isNotEmpty())
    }
    
    @Test
    fun `test version comparison`() {
        // Test that version comparison works correctly for timeline generation
        registry.registerApi(
            methodName = "oldMethod",
            signature = "oldMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "newMethod()",
            deprecationVersion = "4.0.0",
            removalVersion = "5.0.0"
        )
        
        registry.registerApi(
            methodName = "newerMethod",
            signature = "newerMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.MEDIUM,
            removalTimeline = "Q2 2025",
            modernReplacement = "newerMethod()",
            deprecationVersion = "4.5.0",
            removalVersion = "5.5.0"
        )
        
        val report = registry.generateVersionTimelineReport()
        
        assertNotNull("Should return timeline report", report)
        assertTrue("Should have version details", report.versionDetails.isNotEmpty())
        
        // Verify versions are sorted correctly
        val versions = report.versionDetails.map { it.version }
        assertTrue("Should be sorted", versions == versions.sorted())
    }
    
    @Test
    fun `test duplicate registration`() {
        registry.registerApi(
            methodName = "duplicateMethod",
            signature = "duplicateMethod()",
            usageImpact = UsageImpact.LOW,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q1 2025",
            modernReplacement = "newMethod()"
        )
        
        assertEquals("Should have 1 API after first registration", 1, registry.getTotalApiCount())
        
        // Register the same method again
        registry.registerApi(
            methodName = "duplicateMethod",
            signature = "duplicateMethod()",
            usageImpact = UsageImpact.MEDIUM, // Different impact
            complexity = MigrationComplexity.MEDIUM, // Different complexity
            removalTimeline = "Q2 2025",
            modernReplacement = "updatedMethod()"
        )
        
        assertEquals("Should still have 1 API after duplicate registration", 1, registry.getTotalApiCount())
        
        val apiInfo = registry.getApiInfo("duplicateMethod")
        assertNotNull("Should return API info", apiInfo)
        assertEquals("Should have updated impact", UsageImpact.MEDIUM, apiInfo.usageImpact)
        assertEquals("Should have updated complexity", MigrationComplexity.MEDIUM, apiInfo.migrationComplexity)
    }
    
    @Test
    fun `test empty registry operations`() {
        assertEquals("Should have 0 APIs", 0, registry.getTotalApiCount())
        assertTrue("Should have no categories", registry.getAllCategories().isEmpty())
        
        val criticalApis = registry.getApisByImpact(UsageImpact.CRITICAL)
        assertTrue("Should have no critical APIs", criticalApis.isEmpty())
        
        val simpleApis = registry.getApisByComplexity(MigrationComplexity.SIMPLE)
        assertTrue("Should have no simple APIs", simpleApis.isEmpty())
        
        val sessionApis = registry.getApisByCategory("Session Management")
        assertTrue("Should have no session APIs", sessionApis.isEmpty())
    }
    
    @Test
    fun `test migration report with empty usage data`() {
        registry.registerApi(
            methodName = "testMethod",
            signature = "testMethod()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "newMethod()"
        )
        
        val report = registry.generateMigrationReport(emptyMap())
        
        assertNotNull("Should return migration report", report)
        assertEquals("Should have correct total APIs", 1, report.totalApis)
        assertTrue("Should have risk factors", report.riskFactors.isNotEmpty())
        assertTrue("Should have recommendations", report.recommendations.isNotEmpty())
    }
    
    @Test
    fun `test timeline report with no APIs`() {
        val report = registry.generateVersionTimelineReport()
        
        assertNotNull("Should return timeline report", report)
        assertEquals("Should have 0 versions", 0, report.totalVersions)
        assertTrue("Should have no version details", report.versionDetails.isEmpty())
        assertNotNull("Should have summary", report.summary)
    }
} 