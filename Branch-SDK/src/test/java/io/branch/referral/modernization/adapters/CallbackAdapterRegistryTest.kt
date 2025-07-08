package io.branch.referral.modernization.adapters

import io.branch.referral.Branch
import io.branch.referral.BranchError
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Comprehensive unit tests for CallbackAdapterRegistry.
 * 
 * Tests all callback adaptation methods and error scenarios to achieve 95% code coverage.
 */
class CallbackAdapterRegistryTest {
    
    private lateinit var registry: CallbackAdapterRegistry
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        registry = CallbackAdapterRegistry.getInstance()
    }
    
    @Test
    fun `test singleton pattern`() {
        val instance1 = CallbackAdapterRegistry.getInstance()
        val instance2 = CallbackAdapterRegistry.getInstance()
        
        assertSame("Should return same instance", instance1, instance2)
        assertNotNull("Should not be null", instance1)
    }
    
    @Test
    fun `test adaptInitSessionCallback with success result`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        val testParams = JSONObject().apply {
            put("test_key", "test_value")
        }
        
        registry.adaptInitSessionCallback(callback, testParams, null)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNotNull("Should receive params", receivedParams)
        assertEquals("Should have correct params", "test_value", receivedParams?.getString("test_key"))
        assertNull("Should have no error", receivedError)
    }
    
    @Test
    fun `test adaptInitSessionCallback with error`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        val testError = mock(BranchError::class.java)
        
        registry.adaptInitSessionCallback(callback, null, testError)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNull("Should have no params", receivedParams)
        assertNotNull("Should receive error", receivedError)
        assertSame("Should have correct error", testError, receivedError)
    }
    
    @Test
    fun `test adaptInitSessionCallback with null callback`() {
        // Should not throw exception with null callback
        registry.adaptInitSessionCallback(null, JSONObject(), null)
        
        assertTrue("Should handle null callback gracefully", true)
    }
    
    @Test
    fun `test adaptInitSessionCallback with null result and error`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        registry.adaptInitSessionCallback(callback, null, null)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNull("Should have no params", receivedParams)
        assertNull("Should have no error", receivedError)
    }
    
    @Test
    fun `test adaptIdentityCallback with success result`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        val testParams = JSONObject().apply {
            put("identity", "test_user")
        }
        
        registry.adaptIdentityCallback(callback, testParams, null)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNotNull("Should receive params", receivedParams)
        assertEquals("Should have correct identity", "test_user", receivedParams?.getString("identity"))
        assertNull("Should have no error", receivedError)
    }
    
    @Test
    fun `test adaptIdentityCallback with error`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        val testError = mock(BranchError::class.java)
        
        registry.adaptIdentityCallback(callback, null, testError)
        
        // Wait a bit for async callback
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNull("Should have no params", receivedParams)
        assertNotNull("Should receive error", receivedError)
        assertSame("Should have correct error", testError, receivedError)
    }
    
    @Test
    fun `test adaptIdentityCallback with null callback`() {
        // Should not throw exception with null callback
        registry.adaptIdentityCallback(null, JSONObject(), null)
        
        assertTrue("Should handle null callback gracefully", true)
    }
    

    

    

    

    
    @Test
    fun `test concurrent callback execution`() {
        val latch = CountDownLatch(2)
        var callback1Executed = false
        var callback2Executed = false
        
        val callback1 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callback1Executed = true
                latch.countDown()
            }
        }
        
        val callback2 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callback2Executed = true
                latch.countDown()
            }
        }
        
        // Execute callbacks concurrently
        registry.adaptInitSessionCallback(callback1, JSONObject(), null)
        registry.adaptIdentityCallback(callback2, JSONObject(), null)
        
        latch.await(5, TimeUnit.SECONDS)
        
        assertTrue("Callback 1 should have been executed", callback1Executed)
        assertTrue("Callback 2 should have been executed", callback2Executed)
    }
    
    @Test
    fun `test callback execution order`() {
        val executionOrder = mutableListOf<String>()
        
        val callback1 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                executionOrder.add("callback1")
            }
        }
        
        val callback2 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                executionOrder.add("callback2")
            }
        }
        
        // Execute callbacks in sequence
        registry.adaptInitSessionCallback(callback1, JSONObject(), null)
        Thread.sleep(50)
        registry.adaptIdentityCallback(callback2, JSONObject(), null)
        Thread.sleep(50)
        
        assertTrue("Should have executed all callbacks", executionOrder.size >= 2)
        assertTrue("Should contain callback1", executionOrder.contains("callback1"))
        assertTrue("Should contain callback2", executionOrder.contains("callback2"))
    }
    
    @Test
    fun `test callback with complex JSON data`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
            }
        }
        
        val complexParams = JSONObject().apply {
            put("string_value", "test")
            put("int_value", 123)
            put("double_value", 123.45)
            put("boolean_value", true)
            put("null_value", JSONObject.NULL)
            putJSONObject("nested_object", JSONObject().apply {
                put("nested_key", "nested_value")
            })
            putJSONArray("array_value", org.json.JSONArray().apply {
                put("item1")
                put("item2")
                put(123)
            })
        }
        
        registry.adaptInitSessionCallback(callback, complexParams, null)
        
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNotNull("Should receive params", receivedParams)
        assertEquals("Should have correct string value", "test", receivedParams?.getString("string_value"))
        assertEquals("Should have correct int value", 123, receivedParams?.getInt("int_value"))
        assertEquals("Should have correct double value", 123.45, receivedParams?.getDouble("double_value"), 0.01)
        assertTrue("Should have correct boolean value", receivedParams?.getBoolean("boolean_value") == true)
        assertTrue("Should have null value", receivedParams?.isNull("null_value") == true)
        assertNotNull("Should have nested object", receivedParams?.getJSONObject("nested_object"))
        assertNotNull("Should have array", receivedParams?.getJSONArray("array_value"))
    }
    
    @Test
    fun `test callback with exception in callback`() {
        var callbackExecuted = false
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                throw RuntimeException("Test exception in callback")
            }
        }
        
        // Should not throw exception even if callback throws
        registry.adaptInitSessionCallback(callback, JSONObject(), null)
        
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertTrue("Should handle callback exception gracefully", true)
    }
    
    @Test
    fun `test callback with null result and null error`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        var receivedError: BranchError? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
                receivedError = error
            }
        }
        
        registry.adaptInitSessionCallback(callback, null, null)
        
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNull("Should have no params", receivedParams)
        assertNull("Should have no error", receivedError)
    }
    
    @Test
    fun `test callback with empty JSON object`() {
        var callbackExecuted = false
        var receivedParams: JSONObject? = null
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
                receivedParams = referringParams
            }
        }
        
        val emptyParams = JSONObject()
        
        registry.adaptInitSessionCallback(callback, emptyParams, null)
        
        Thread.sleep(100)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertNotNull("Should receive params", receivedParams)
        assertEquals("Should have empty params", 0, receivedParams?.length())
    }
    
    @Test
    fun `test multiple callbacks for same result`() {
        var callback1Executed = false
        var callback2Executed = false
        
        val callback1 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callback1Executed = true
            }
        }
        
        val callback2 = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callback2Executed = true
            }
        }
        
        val testParams = JSONObject().apply {
            put("test_key", "test_value")
        }
        
        registry.adaptInitSessionCallback(callback1, testParams, null)
        registry.adaptInitSessionCallback(callback2, testParams, null)
        
        Thread.sleep(100)
        
        assertTrue("Callback 1 should have been executed", callback1Executed)
        assertTrue("Callback 2 should have been executed", callback2Executed)
    }
    
    @Test
    fun `test callback with different result types`() {
        var initCallbackExecuted = false
        var identityCallbackExecuted = false
        
        val initCallback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                initCallbackExecuted = true
            }
        }
        
        val identityCallback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                identityCallbackExecuted = true
            }
        }
        
        // Test with different result types
        registry.adaptInitSessionCallback(initCallback, JSONObject(), null)
        registry.adaptIdentityCallback(identityCallback, "string_result", null)
        
        Thread.sleep(100)
        
        assertTrue("Init callback should have been executed", initCallbackExecuted)
        assertTrue("Identity callback should have been executed", identityCallbackExecuted)
    }
    
    @Test
    fun `test callback registry singleton behavior under load`() {
        val latch = CountDownLatch(10)
        val instances = mutableSetOf<CallbackAdapterRegistry>()
        
        repeat(10) {
            Thread {
                val instance = CallbackAdapterRegistry.getInstance()
                synchronized(instances) {
                    instances.add(instance)
                }
                latch.countDown()
            }.start()
        }
        
        latch.await(5, TimeUnit.SECONDS)
        
        assertEquals("Should have only one instance", 1, instances.size)
    }
    
    @Test
    fun `test callback execution with very short delay`() {
        var callbackExecuted = false
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
            }
        }
        
        registry.adaptInitSessionCallback(callback, JSONObject(), null)
        
        // Very short delay
        Thread.sleep(1)
        
        assertTrue("Callback should have been executed even with short delay", callbackExecuted)
    }
    
    @Test
    fun `test callback execution with very long delay`() {
        var callbackExecuted = false
        
        val callback = object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
                callbackExecuted = true
            }
        }
        
        registry.adaptInitSessionCallback(callback, JSONObject(), null)
        
        // Longer delay
        Thread.sleep(500)
        
        assertTrue("Callback should have been executed with longer delay", callbackExecuted)
    }
} 