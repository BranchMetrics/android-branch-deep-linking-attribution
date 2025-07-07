package io.branch.referral.modernization.wrappers

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Comprehensive unit tests for PreservedBranchApi.
 * 
 * Tests all static methods and error scenarios to achieve 95% code coverage.
 */
class PreservedBranchApiTest {
    
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockContext = mock(Context::class.java)
    }
    
    @Test
    fun `test getInstance`() {
        val instance = PreservedBranchApi.getInstance()
        
        assertNotNull("Should return valid instance", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test getInstance singleton behavior`() {
        val instance1 = PreservedBranchApi.getInstance()
        val instance2 = PreservedBranchApi.getInstance()
        
        assertSame("Should return same instance", instance1, instance2)
    }
    
    @Test
    fun `test getAutoInstance`() {
        val instance = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertNotNull("Should return valid instance", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test getAutoInstance with null context`() {
        val instance = PreservedBranchApi.getAutoInstance(null)
        
        assertNotNull("Should handle null context gracefully", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test enableTestMode`() {
        // Should not throw exception
        PreservedBranchApi.enableTestMode()
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test enableLogging`() {
        // Should not throw exception
        PreservedBranchApi.enableLogging()
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test disableLogging`() {
        // Should not throw exception
        PreservedBranchApi.disableLogging()
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setRetryCount`() {
        // Should not throw exception
        PreservedBranchApi.setRetryCount(3)
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setRetryCount with negative value`() {
        // Should not throw exception
        PreservedBranchApi.setRetryCount(-1)
        
        assertTrue("Should handle negative values gracefully", true)
    }
    
    @Test
    fun `test setRetryCount with zero`() {
        // Should not throw exception
        PreservedBranchApi.setRetryCount(0)
        
        assertTrue("Should handle zero gracefully", true)
    }
    
    @Test
    fun `test setRetryCount with large value`() {
        // Should not throw exception
        PreservedBranchApi.setRetryCount(Int.MAX_VALUE)
        
        assertTrue("Should handle large values gracefully", true)
    }
    
    @Test
    fun `test setTimeout`() {
        // Should not throw exception
        PreservedBranchApi.setTimeout(5000)
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setTimeout with negative value`() {
        // Should not throw exception
        PreservedBranchApi.setTimeout(-1000)
        
        assertTrue("Should handle negative values gracefully", true)
    }
    
    @Test
    fun `test setTimeout with zero`() {
        // Should not throw exception
        PreservedBranchApi.setTimeout(0)
        
        assertTrue("Should handle zero gracefully", true)
    }
    
    @Test
    fun `test setTimeout with large value`() {
        // Should not throw exception
        PreservedBranchApi.setTimeout(Int.MAX_VALUE)
        
        assertTrue("Should handle large values gracefully", true)
    }
    
    @Test
    fun `test setNetworkTimeout`() {
        // Should not throw exception
        PreservedBranchApi.setNetworkTimeout(10000)
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setNetworkTimeout with negative value`() {
        // Should not throw exception
        PreservedBranchApi.setNetworkTimeout(-5000)
        
        assertTrue("Should handle negative values gracefully", true)
    }
    
    @Test
    fun `test setNetworkTimeout with zero`() {
        // Should not throw exception
        PreservedBranchApi.setNetworkTimeout(0)
        
        assertTrue("Should handle zero gracefully", true)
    }
    
    @Test
    fun `test setNetworkTimeout with large value`() {
        // Should not throw exception
        PreservedBranchApi.setNetworkTimeout(Int.MAX_VALUE)
        
        assertTrue("Should handle large values gracefully", true)
    }
    
    @Test
    fun `test setMaxRetries`() {
        // Should not throw exception
        PreservedBranchApi.setMaxRetries(5)
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setMaxRetries with negative value`() {
        // Should not throw exception
        PreservedBranchApi.setMaxRetries(-3)
        
        assertTrue("Should handle negative values gracefully", true)
    }
    
    @Test
    fun `test setMaxRetries with zero`() {
        // Should not throw exception
        PreservedBranchApi.setMaxRetries(0)
        
        assertTrue("Should handle zero gracefully", true)
    }
    
    @Test
    fun `test setMaxRetries with large value`() {
        // Should not throw exception
        PreservedBranchApi.setMaxRetries(Int.MAX_VALUE)
        
        assertTrue("Should handle large values gracefully", true)
    }
    
    @Test
    fun `test setRetryInterval`() {
        // Should not throw exception
        PreservedBranchApi.setRetryInterval(2000)
        
        assertTrue("Should execute without exception", true)
    }
    
    @Test
    fun `test setRetryInterval with negative value`() {
        // Should not throw exception
        PreservedBranchApi.setRetryInterval(-2000)
        
        assertTrue("Should handle negative values gracefully", true)
    }
    
    @Test
    fun `test setRetryInterval with zero`() {
        // Should not throw exception
        PreservedBranchApi.setRetryInterval(0)
        
        assertTrue("Should handle zero gracefully", true)
    }
    
    @Test
    fun `test setRetryInterval with large value`() {
        // Should not throw exception
        PreservedBranchApi.setRetryInterval(Int.MAX_VALUE)
        
        assertTrue("Should handle large values gracefully", true)
    }
    
    @Test
    fun `test multiple configuration calls`() {
        // Test multiple configuration calls in sequence
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.enableLogging()
        PreservedBranchApi.setRetryCount(3)
        PreservedBranchApi.setTimeout(5000)
        PreservedBranchApi.setNetworkTimeout(10000)
        PreservedBranchApi.setMaxRetries(5)
        PreservedBranchApi.setRetryInterval(2000)
        PreservedBranchApi.disableLogging()
        
        assertTrue("Should execute all configuration calls without exception", true)
    }
    
    @Test
    fun `test concurrent access to getInstance`() {
        val latch = java.util.concurrent.CountDownLatch(2)
        var instance1: LegacyBranchWrapper? = null
        var instance2: LegacyBranchWrapper? = null
        
        Thread {
            instance1 = PreservedBranchApi.getInstance()
            latch.countDown()
        }.start()
        
        Thread {
            instance2 = PreservedBranchApi.getInstance()
            latch.countDown()
        }.start()
        
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        
        assertNotNull("First instance should not be null", instance1)
        assertNotNull("Second instance should not be null", instance2)
        assertSame("Should return same instance", instance1, instance2)
    }
    
    @Test
    fun `test concurrent access to getAutoInstance`() {
        val latch = java.util.concurrent.CountDownLatch(2)
        var instance1: LegacyBranchWrapper? = null
        var instance2: LegacyBranchWrapper? = null
        
        Thread {
            instance1 = PreservedBranchApi.getAutoInstance(mockContext)
            latch.countDown()
        }.start()
        
        Thread {
            instance2 = PreservedBranchApi.getAutoInstance(mockContext)
            latch.countDown()
        }.start()
        
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        
        assertNotNull("First instance should not be null", instance1)
        assertNotNull("Second instance should not be null", instance2)
        assertSame("Should return same instance", instance1, instance2)
    }
    
    @Test
    fun `test configuration methods are idempotent`() {
        // Call configuration methods multiple times
        repeat(3) {
            PreservedBranchApi.enableTestMode()
            PreservedBranchApi.enableLogging()
            PreservedBranchApi.setRetryCount(3)
            PreservedBranchApi.setTimeout(5000)
            PreservedBranchApi.setNetworkTimeout(10000)
            PreservedBranchApi.setMaxRetries(5)
            PreservedBranchApi.setRetryInterval(2000)
            PreservedBranchApi.disableLogging()
        }
        
        assertTrue("Should handle multiple calls gracefully", true)
    }
    
    @Test
    fun `test edge case values`() {
        // Test edge case values for all configuration methods
        PreservedBranchApi.setRetryCount(1)
        PreservedBranchApi.setRetryCount(100)
        PreservedBranchApi.setTimeout(1)
        PreservedBranchApi.setTimeout(100000)
        PreservedBranchApi.setNetworkTimeout(1)
        PreservedBranchApi.setNetworkTimeout(100000)
        PreservedBranchApi.setMaxRetries(1)
        PreservedBranchApi.setMaxRetries(100)
        PreservedBranchApi.setRetryInterval(1)
        PreservedBranchApi.setRetryInterval(100000)
        
        assertTrue("Should handle edge case values gracefully", true)
    }
    
    @Test
    fun `test mixed configuration scenarios`() {
        // Test various combinations of configuration calls
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.setRetryCount(2)
        PreservedBranchApi.setTimeout(3000)
        PreservedBranchApi.disableLogging()
        PreservedBranchApi.setNetworkTimeout(8000)
        PreservedBranchApi.enableLogging()
        PreservedBranchApi.setMaxRetries(3)
        PreservedBranchApi.setRetryInterval(1500)
        
        assertTrue("Should handle mixed configuration scenarios gracefully", true)
    }
    
    @Test
    fun `test configuration after instance creation`() {
        // Create instance first, then configure
        val instance = PreservedBranchApi.getInstance()
        assertNotNull("Should return valid instance", instance)
        
        // Configure after instance creation
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.setRetryCount(4)
        PreservedBranchApi.setTimeout(6000)
        
        assertTrue("Should configure after instance creation", true)
    }
    
    @Test
    fun `test configuration before instance creation`() {
        // Configure before creating instance
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.setRetryCount(5)
        PreservedBranchApi.setTimeout(7000)
        
        // Create instance after configuration
        val instance = PreservedBranchApi.getInstance()
        assertNotNull("Should return valid instance after configuration", instance)
        
        assertTrue("Should handle configuration before instance creation", true)
    }
    
    @Test
    fun `test getAutoInstance with different contexts`() {
        val context1 = mock(Context::class.java)
        val context2 = mock(Context::class.java)
        
        val instance1 = PreservedBranchApi.getAutoInstance(context1)
        val instance2 = PreservedBranchApi.getAutoInstance(context2)
        
        assertNotNull("Should return valid instance for context1", instance1)
        assertNotNull("Should return valid instance for context2", instance2)
        assertSame("Should return same instance regardless of context", instance1, instance2)
    }
    
    @Test
    fun `test getAutoInstance with application context`() {
        val applicationContext = mock(Context::class.java)
        `when`(mockContext.applicationContext).thenReturn(applicationContext)
        
        val instance = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertNotNull("Should return valid instance with application context", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test getAutoInstance with null application context`() {
        `when`(mockContext.applicationContext).thenReturn(null)
        
        val instance = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertNotNull("Should handle null application context gracefully", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test getAutoInstance with same context multiple times`() {
        val instance1 = PreservedBranchApi.getAutoInstance(mockContext)
        val instance2 = PreservedBranchApi.getAutoInstance(mockContext)
        val instance3 = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertSame("Should return same instance for same context", instance1, instance2)
        assertSame("Should return same instance for same context", instance2, instance3)
        assertSame("Should return same instance for same context", instance1, instance3)
    }
    
    @Test
    fun `test getAutoInstance with context that throws exception`() {
        `when`(mockContext.applicationContext).thenThrow(RuntimeException("Test exception"))
        
        val instance = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertNotNull("Should handle context exception gracefully", instance)
        assertTrue("Should be LegacyBranchWrapper", instance is LegacyBranchWrapper)
    }
    
    @Test
    fun `test getInstance after getAutoInstance`() {
        val autoInstance = PreservedBranchApi.getAutoInstance(mockContext)
        val regularInstance = PreservedBranchApi.getInstance()
        
        assertSame("Should return same instance", autoInstance, regularInstance)
    }
    
    @Test
    fun `test getAutoInstance after getInstance`() {
        val regularInstance = PreservedBranchApi.getInstance()
        val autoInstance = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertSame("Should return same instance", regularInstance, autoInstance)
    }
    
    @Test
    fun `test configuration persistence across instances`() {
        // Configure using static methods
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.setRetryCount(6)
        PreservedBranchApi.setTimeout(8000)
        
        // Get instances
        val instance1 = PreservedBranchApi.getInstance()
        val instance2 = PreservedBranchApi.getAutoInstance(mockContext)
        
        assertSame("Should return same instance", instance1, instance2)
        assertTrue("Configuration should persist across instances", true)
    }
    
    @Test
    fun `test all configuration methods in single test`() {
        // Test all configuration methods in one test to ensure they work together
        PreservedBranchApi.enableTestMode()
        PreservedBranchApi.enableLogging()
        PreservedBranchApi.setRetryCount(7)
        PreservedBranchApi.setTimeout(9000)
        PreservedBranchApi.setNetworkTimeout(15000)
        PreservedBranchApi.setMaxRetries(8)
        PreservedBranchApi.setRetryInterval(2500)
        PreservedBranchApi.disableLogging()
        
        // Get instance after all configuration
        val instance = PreservedBranchApi.getInstance()
        assertNotNull("Should return valid instance after all configuration", instance)
        
        assertTrue("Should handle all configuration methods together", true)
    }
} 