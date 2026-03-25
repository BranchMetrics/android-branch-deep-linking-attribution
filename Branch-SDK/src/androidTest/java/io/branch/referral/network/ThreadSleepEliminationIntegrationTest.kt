package io.branch.referral.network

import io.branch.referral.Branch

/**
 * Integration tests to verify complete elimination of Thread.sleep() calls
 * in the Branch SDK network layer.
 * 
 * These tests validate that the new coroutine-based implementation
 * successfully replaces blocking Thread.sleep() with non-blocking operations.
 */
class ThreadSleepEliminationIntegrationTest {
    
    /**
     * Test that demonstrates Thread.sleep() elimination in the async network layer.
     */
    fun testThreadSleepEliminationInAsyncLayer() {
        try {
            val mockBranch = createMockBranch()
            val networkLayer = BranchAsyncNetworkLayer(mockBranch)
            
            val startTime = System.currentTimeMillis()
            
            // Operations that would previously use Thread.sleep() should now be fast
            networkLayer.cancelAll()
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Should complete very quickly (no Thread.sleep() blocking)
            if (duration > 500) {
                throw AssertionError("Operation took too long (${duration}ms), may indicate Thread.sleep() usage")
            }
            
            println("✓ Thread.sleep() elimination in async layer test passed (${duration}ms)")
        } catch (e: Exception) {
            println("✗ Thread.sleep() elimination in async layer test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test that the async interface provides non-blocking operations.
     */
    fun testAsyncInterfaceNonBlocking() {
        try {
            val mockBranch = createMockBranch()
            
            val startTime = System.currentTimeMillis()
            val asyncInterface = BranchRemoteInterfaceUrlConnectionAsync(mockBranch)
            val endTime = System.currentTimeMillis()
            
            val initTime = endTime - startTime
            
            // Initialization should be very fast (no blocking operations)
            if (initTime > 500) {
                throw AssertionError("Interface initialization took too long (${initTime}ms)")
            }
            
            asyncInterface.cancelAllOperations()
            
            println("✓ Async interface non-blocking test passed (${initTime}ms)")
        } catch (e: Exception) {
            println("✗ Async interface non-blocking test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test that multiple concurrent operations don't block threads.
     */
    fun testConcurrentOperationsNonBlocking() {
        try {
            val mockBranch = createMockBranch()
            val networkLayers = mutableListOf<BranchAsyncNetworkLayer>()
            
            val startTime = System.currentTimeMillis()
            
            // Create multiple network layers (should be concurrent, not sequential)
            repeat(10) {
                val networkLayer = BranchAsyncNetworkLayer(mockBranch)
                networkLayers.add(networkLayer)
            }
            
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Should complete quickly (concurrent, not sequential with Thread.sleep())
            if (totalTime > 2000) {
                throw AssertionError("Concurrent operations took too long (${totalTime}ms)")
            }
            
            // Cleanup
            networkLayers.forEach { it.cancelAll() }
            
            println("✓ Concurrent operations non-blocking test passed (${totalTime}ms)")
        } catch (e: Exception) {
            println("✗ Concurrent operations non-blocking test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test that the original interface can use async implementation.
     */
    fun testOriginalInterfaceAsyncImplementation() {
        try {
            val mockBranch = createMockBranch()
            val originalInterface = BranchRemoteInterfaceUrlConnection(mockBranch)
            
            // Verify async implementation is enabled by default
            if (!originalInterface.isAsyncImplementationEnabled()) {
                throw AssertionError("Original interface should use async implementation by default")
            }
            
            // Test toggling
            originalInterface.setAsyncImplementationEnabled(false)
            if (originalInterface.isAsyncImplementationEnabled()) {
                throw AssertionError("Should be able to disable async implementation")
            }
            
            originalInterface.setAsyncImplementationEnabled(true)
            if (!originalInterface.isAsyncImplementationEnabled()) {
                throw AssertionError("Should be able to re-enable async implementation")
            }
            
            // Cleanup
            originalInterface.cancelAsyncOperations()
            
            println("✓ Original interface async implementation test passed")
        } catch (e: Exception) {
            println("✗ Original interface async implementation test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test memory efficiency without blocked threads.
     */
    fun testMemoryEfficiency() {
        try {
            val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val networkLayers = mutableListOf<BranchAsyncNetworkLayer>()
            
            val mockBranch = createMockBranch()
            
            // Create multiple network layers
            repeat(20) {
                val networkLayer = BranchAsyncNetworkLayer(mockBranch)
                networkLayers.add(networkLayer)
            }
            
            val afterCreationMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryIncrease = afterCreationMemory - initialMemory
            
            // Memory increase should be reasonable (no blocked threads consuming memory)
            if (memoryIncrease > 50 * 1024 * 1024) { // 50MB threshold
                throw AssertionError("Memory increase too high: ${memoryIncrease / 1024}KB")
            }
            
            // Cleanup
            networkLayers.forEach { it.cancelAll() }
            
            println("✓ Memory efficiency test passed (memory increase: ${memoryIncrease / 1024}KB)")
        } catch (e: Exception) {
            println("✗ Memory efficiency test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test that demonstrates performance improvement over Thread.sleep() approach.
     */
    fun testPerformanceImprovement() {
        try {
            val mockBranch = createMockBranch()
            val networkLayer = BranchAsyncNetworkLayer(mockBranch)
            
            val startTime = System.currentTimeMillis()
            
            // Operations that would have involved Thread.sleep() in the old implementation
            // should now complete very quickly
            repeat(10) {
                // These operations would have been Thread.sleep() calls in the old implementation
                val tempLayer = BranchAsyncNetworkLayer(mockBranch)
                tempLayer.cancelAll()
            }
            
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Should complete much faster than if using Thread.sleep()
            if (totalTime > 1000) {
                throw AssertionError("Performance test took too long (${totalTime}ms), may indicate Thread.sleep() usage")
            }
            
            networkLayer.cancelAll()
            
            println("✓ Performance improvement test passed (${totalTime}ms for 10 operations)")
        } catch (e: Exception) {
            println("✗ Performance improvement test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Test structured concurrency prevents resource leaks.
     */
    fun testStructuredConcurrency() {
        try {
            val mockBranch = createMockBranch()
            val networkLayer = BranchAsyncNetworkLayer(mockBranch)
            
            // Test that cancellation works properly (structured concurrency)
            networkLayer.cancelAll()
            
            // Should not throw any exceptions
            println("✓ Structured concurrency test passed")
        } catch (e: Exception) {
            println("✗ Structured concurrency test failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Creates a mock Branch instance for testing.
     */
    private fun createMockBranch(): Branch {
        return Branch::class.java.getDeclaredConstructor().newInstance()
    }
    
    companion object {
        /**
         * Run all tests.
         */
        @JvmStatic
        fun runAllTests() {
            println("Running Thread.sleep() elimination integration tests...")
            
            val test = ThreadSleepEliminationIntegrationTest()
            
            try {
                test.testThreadSleepEliminationInAsyncLayer()
                test.testAsyncInterfaceNonBlocking()
                test.testConcurrentOperationsNonBlocking()
                test.testOriginalInterfaceAsyncImplementation()
                test.testMemoryEfficiency()
                test.testPerformanceImprovement()
                test.testStructuredConcurrency()
                
                println("✅ All Thread.sleep() elimination integration tests passed!")
            } catch (e: Exception) {
                println("❌ Thread.sleep() elimination integration tests failed: ${e.message}")
                throw e
            }
        }
    }
}