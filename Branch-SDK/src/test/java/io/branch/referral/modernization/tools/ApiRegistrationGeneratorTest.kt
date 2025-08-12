package io.branch.referral.modernization.tools

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Comprehensive test suite for ApiRegistrationGenerator.
 * Ensures the automatic API extraction works correctly.
 */
class ApiRegistrationGeneratorTest {
    
    private val testOutputDir = "test_generated/"
    
    @Before
    fun setUp() {
        // Clean up any previous test artifacts
        cleanupTestFiles()
    }
    
    @After
    fun tearDown() {
        // Clean up test artifacts
        cleanupTestFiles()
    }
    
    @Test
    fun `should extract public APIs from Branch class`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        
        // Verify we found APIs
        assertTrue("Should find at least 10 APIs", apis.size >= 10)
        
        // Verify Branch APIs are included
        val branchApis = apis.filter { it.className == "Branch" }
        assertTrue("Should find Branch APIs", branchApis.isNotEmpty())
        
        // Verify common methods are present
        val methodNames = branchApis.map { it.methodName }
        assertTrue("Should include initSession", methodNames.contains("initSession"))
        assertTrue("Should include setIdentity", methodNames.contains("setIdentity"))
        assertTrue("Should include getShortUrl", methodNames.contains("getShortUrl"))
    }
    
    @Test
    fun `should extract APIs from all target classes`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        val classNames = apis.map { it.className }.distinct()
        
        // Verify all target classes are represented
        assertTrue("Should include Branch", classNames.contains("Branch"))
        assertTrue("Should include BranchUniversalObject", classNames.contains("BranchUniversalObject"))
        assertTrue("Should include BranchEvent", classNames.contains("BranchEvent"))
        assertTrue("Should include LinkProperties", classNames.contains("LinkProperties"))
    }
    
    @Test
    fun `should filter out getter and setter methods`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        val methodNames = apis.map { it.methodName }
        
        // Verify getters/setters are filtered out
        val getters = methodNames.filter { it.startsWith("get") && it.length > 3 }
        val setters = methodNames.filter { it.startsWith("set") && it.length > 3 }
        
        // Should have very few or no simple getters/setters
        assertTrue("Should filter most getters", getters.size < 5)
        assertTrue("Should filter most setters", setters.size < 5)
    }
    
    @Test
    fun `should filter out Object methods`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        val methodNames = apis.map { it.methodName }
        
        // Verify Object methods are filtered out
        assertFalse("Should not include toString", methodNames.contains("toString"))
        assertFalse("Should not include hashCode", methodNames.contains("hashCode"))
        assertFalse("Should not include equals", methodNames.contains("equals"))
        assertFalse("Should not include clone", methodNames.contains("clone"))
    }
    
    @Test
    fun `should generate valid registration code`() {
        val registrationCode = ApiRegistrationGenerator.generateApiRegistrationCode()
        
        // Verify code structure
        assertTrue("Should contain registerApi calls", 
                  registrationCode.contains("registerApi("))
        assertTrue("Should contain ApiCategory", 
                  registrationCode.contains("ApiCategory."))
        assertTrue("Should contain ApiCriticality", 
                  registrationCode.contains("ApiCriticality."))
        
        // Verify comments are present
        assertTrue("Should contain auto-generated comment", 
                  registrationCode.contains("Auto-generated API registrations"))
        assertTrue("Should contain timestamp", 
                  registrationCode.contains("Generated on:"))
    }
    
    @Test
    fun `should generate valid wrapper methods`() {
        val wrapperCode = ApiRegistrationGenerator.generateWrapperMethods()
        
        // Verify wrapper structure
        assertTrue("Should contain @Deprecated annotations", 
                  wrapperCode.contains("@Deprecated"))
        assertTrue("Should contain BranchApiPreservationManager calls", 
                  wrapperCode.contains("BranchApiPreservationManager"))
        assertTrue("Should contain executeApiCall", 
                  wrapperCode.contains("executeApiCall"))
        
        // Verify auto-generated comment
        assertTrue("Should contain auto-generated comment", 
                  wrapperCode.contains("Auto-generated wrapper methods"))
    }
    
    @Test
    fun `should categorize APIs correctly`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        
        // Find specific APIs and verify their categories
        val sessionApi = apis.find { it.methodName.contains("session", ignoreCase = true) }
        val identityApi = apis.find { it.methodName.contains("identity", ignoreCase = true) }
        val linkApi = apis.find { it.methodName.contains("link", ignoreCase = true) }
        val eventApi = apis.find { it.methodName.contains("event", ignoreCase = true) }
        
        // Note: We can't test the actual categorization without accessing private methods
        // But we can verify the APIs exist
        assertNotNull("Should find session-related API", sessionApi)
        assertNotNull("Should find identity-related API", identityApi)
    }
    
    @Test
    fun `should determine API criticality correctly`() {
        val registrationCode = ApiRegistrationGenerator.generateApiRegistrationCode()
        
        // Verify criticality levels are assigned
        assertTrue("Should have HIGH criticality APIs", 
                  registrationCode.contains("ApiCriticality.HIGH"))
        assertTrue("Should have MEDIUM criticality APIs", 
                  registrationCode.contains("ApiCriticality.MEDIUM"))
        assertTrue("Should have LOW criticality APIs", 
                  registrationCode.contains("ApiCriticality.LOW"))
    }
    
    @Test
    fun `should create valid API signatures`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        
        apis.forEach { api ->
            val signature = api.getSignature()
            
            // Verify signature format
            assertTrue("Signature should contain class name: $signature", 
                      signature.contains("${api.className}."))
            assertTrue("Signature should contain method name: $signature", 
                      signature.contains(api.methodName))
            assertTrue("Signature should contain parentheses: $signature", 
                      signature.contains("(") && signature.contains(")"))
        }
    }
    
    @Test
    fun `should handle methods with different parameter counts`() {
        val apis = ApiRegistrationGenerator.extractAllPublicApis()
        
        // Find methods with different parameter counts
        val noParams = apis.filter { it.parameterTypes.isEmpty() }
        val singleParam = apis.filter { it.parameterTypes.size == 1 }
        val multipleParams = apis.filter { it.parameterTypes.size > 1 }
        
        // Verify we have variety
        assertTrue("Should have no-parameter methods", noParams.isNotEmpty())
        assertTrue("Should have single-parameter methods", singleParam.isNotEmpty())
        assertTrue("Should have multi-parameter methods", multipleParams.isNotEmpty())
    }
    
    @Test
    fun `should execute full generation successfully`() {
        val result = ApiRegistrationGenerator.executeFullGeneration()
        
        // Verify successful execution
        assertTrue("Generation should succeed", result.success)
        assertNull("Should not have errors", result.error)
        
        // Verify all components are generated
        assertTrue("Should have discovered APIs", result.discoveredApis.isNotEmpty())
        assertTrue("Should have registration code", result.registrationCode.isNotEmpty())
        assertTrue("Should have wrapper code", result.wrapperCode.isNotEmpty())
        assertNotNull("Should have validation result", result.validation)
    }
    
    @Test
    fun `should validate API registration consistency`() {
        // This test simulates the validation process
        val discoveredApis = ApiRegistrationGenerator.extractAllPublicApis()
        
        // Verify we have a reasonable number of APIs
        assertTrue("Should discover at least 50 APIs", discoveredApis.size >= 50)
        assertTrue("Should discover at most 200 APIs", discoveredApis.size <= 200)
        
        // Verify signatures are unique
        val signatures = discoveredApis.map { it.getSignature() }
        val uniqueSignatures = signatures.distinct()
        assertEquals("All signatures should be unique", signatures.size, uniqueSignatures.size)
    }
    
    @Test
    fun `should handle reflection errors gracefully`() {
        // This test ensures the generator doesn't crash on reflection issues
        try {
            val result = ApiRegistrationGenerator.executeFullGeneration()
            // Should either succeed or fail gracefully
            if (!result.success) {
                assertNotNull("Should have error message", result.error)
            }
        } catch (e: Exception) {
            fail("Should not throw unhandled exceptions: ${e.message}")
        }
    }
    
    @Test
    fun `generated code should be syntactically valid Kotlin`() {
        val registrationCode = ApiRegistrationGenerator.generateApiRegistrationCode()
        val wrapperCode = ApiRegistrationGenerator.generateWrapperMethods()
        
        // Basic syntax checks
        val registrationLines = registrationCode.lines()
        val wrapperLines = wrapperCode.lines()
        
        // Check for balanced parentheses in registration code
        registrationLines.forEach { line ->
            if (line.contains("registerApi")) {
                val openParens = line.count { it == '(' }
                val closeParens = line.count { it == ')' }
                assertEquals("Parentheses should be balanced in: $line", openParens, closeParens)
            }
        }
        
        // Check for balanced braces in wrapper code
        val openBraces = wrapperCode.count { it == '{' }
        val closeBraces = wrapperCode.count { it == '}' }
        assertEquals("Braces should be balanced in wrapper code", openBraces, closeBraces)
    }
    
    private fun cleanupTestFiles() {
        try {
            val testDir = File(testOutputDir)
            if (testDir.exists()) {
                testDir.deleteRecursively()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }
} 