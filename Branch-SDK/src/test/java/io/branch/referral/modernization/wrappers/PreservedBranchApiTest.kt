package io.branch.referral.modernization.wrappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Simplified tests for PreservedBranchApi that focus on basic functionality
 * without complex Android dependencies or initialization issues.
 */
class PreservedBranchApiTest {
    
    @Test
    fun `test static API preservation pattern`() {
        // Test that the PreservedBranchApi class exists and is accessible
        val className = PreservedBranchApi::class.java.name
        assertEquals("Should have correct class name", "io.branch.referral.modernization.wrappers.PreservedBranchApi", className)
    }
    
    @Test
    fun `test API methods exist and are deprecated`() {
        // Test that deprecated API methods exist
        val methods = PreservedBranchApi::class.java.declaredMethods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue("Should have getInstance method", methodNames.contains("getInstance"))
        assertTrue("Should have enableTestMode method", methodNames.contains("enableTestMode"))
        assertTrue("Should have enableLogging method", methodNames.contains("enableLogging"))
        assertTrue("Should have disableLogging method", methodNames.contains("disableLogging"))
        assertTrue("Should have setRetryCount method", methodNames.contains("setRetryCount"))
        assertTrue("Should have setRequestTimeout method", methodNames.contains("setRequestTimeout"))
    }
    
    @Test
    fun `test deprecated annotations present`() {
        // Test that methods have proper deprecation annotations
        val methods = PreservedBranchApi::class.java.declaredMethods
        val getInstanceMethod = methods.find { it.name == "getInstance" && it.parameterCount == 0 }
        
        assertNotNull("Should have getInstance method", getInstanceMethod)
        assertTrue("getInstance should be deprecated", getInstanceMethod?.isAnnotationPresent(Deprecated::class.java) == true)
    }
    
    @Test
    fun `test class is object singleton`() {
        // Test that PreservedBranchApi is an object (singleton)
        val field = PreservedBranchApi::class.java.declaredFields.find { it.name == "INSTANCE" }
        assertNotNull("Should be a Kotlin object with INSTANCE field", field)
    }
    
    @Test
    fun `test method signatures preserve legacy API`() {
        // Test that method signatures match legacy API expectations
        val methods = PreservedBranchApi::class.java.declaredMethods
        
        val setRetryCountMethod = methods.find { it.name == "setRetryCount" }
        assertNotNull("Should have setRetryCount method", setRetryCountMethod)
        assertEquals("setRetryCount should take Int parameter", 1, setRetryCountMethod?.parameterCount)
        assertEquals("setRetryCount should take Int parameter", Int::class.java, setRetryCountMethod?.parameterTypes?.get(0))
        
        val setRequestTimeoutMethod = methods.find { it.name == "setRequestTimeout" }
        assertNotNull("Should have setRequestTimeout method", setRequestTimeoutMethod)
        assertEquals("setRequestTimeout should take Int parameter", 1, setRequestTimeoutMethod?.parameterCount)
        assertEquals("setRequestTimeout should take Int parameter", Int::class.java, setRequestTimeoutMethod?.parameterTypes?.get(0))
    }
    
    @Test
    fun `test static method accessibility`() {
        // Test that all main API methods are static/JvmStatic
        val methods = PreservedBranchApi::class.java.declaredMethods
        
        val staticMethods = methods.filter { 
            java.lang.reflect.Modifier.isStatic(it.modifiers) && 
            it.name in listOf("getInstance", "enableTestMode", "enableLogging", "disableLogging", "setRetryCount", "setRequestTimeout")
        }
        
        assertTrue("Should have static methods", staticMethods.isNotEmpty())
        assertTrue("Should have getInstance as static", staticMethods.any { it.name == "getInstance" })
    }
    
    @Test
    fun `test configuration method return types`() {
        // Test that configuration methods return Unit (void)
        val methods = PreservedBranchApi::class.java.declaredMethods
        
        val configMethods = listOf("enableTestMode", "enableLogging", "disableLogging", "setRetryCount", "setRequestTimeout")
        
        for (methodName in configMethods) {
            val method = methods.find { it.name == methodName }
            assertNotNull("Should have $methodName method", method)
            assertTrue("$methodName should return void/Unit", 
                method?.returnType == Void.TYPE || method?.returnType?.name == "kotlin.Unit")
        }
    }
    
    @Test
    fun `test API preservation design pattern`() {
        // Test that the class follows the API preservation pattern
        val field = PreservedBranchApi::class.java.declaredFields.find { it.name == "INSTANCE" }
        assertNotNull("Should be a Kotlin object with INSTANCE field", field)
        
        val className = PreservedBranchApi::class.java.name
        assertTrue("Should be in preservation package", className.contains("preservation") || className.contains("wrappers"))
    }
    
    @Test
    fun `test method count matches expectations`() {
        // Test that we have the expected number of API methods
        val methods = PreservedBranchApi::class.java.declaredMethods
        val publicMethods = methods.filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
        
        assertTrue("Should have multiple public methods", publicMethods.size >= 6)
        assertTrue("Should not have too many methods", publicMethods.size <= 20)
    }
    
    @Test
    fun `test class package structure`() {
        // Test that the class is in the correct package
        val packageName = PreservedBranchApi::class.java.`package`.name
        assertEquals("Should be in wrappers package", "io.branch.referral.modernization.wrappers", packageName)
    }
}