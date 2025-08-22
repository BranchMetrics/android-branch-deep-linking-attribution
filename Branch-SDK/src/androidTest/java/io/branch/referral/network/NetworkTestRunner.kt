package io.branch.referral.network

/**
 * Simple test runner for network layer tests.
 * 
 * This runner executes all network-related tests to verify that the
 * Thread.sleep() elimination implementation works correctly.
 */
object NetworkTestRunner {
    
    /**
     * Run all network layer tests.
     */
    @JvmStatic
    fun runAllNetworkTests() {
        println("🚀 Starting Branch SDK Network Layer Tests")
        println("=========================================")
        println("Testing Thread.sleep() elimination and coroutine-based implementation")
        println()
        
        var allTestsPassed = true
        
        try {
            // Run BranchAsyncNetworkLayer tests
            println("📋 Running BranchAsyncNetworkLayer tests...")
            BranchAsyncNetworkLayerTest.runAllTests()
            println()
            
            // Run BranchRemoteInterfaceUrlConnectionAsync tests
            println("📋 Running BranchRemoteInterfaceUrlConnectionAsync tests...")
            BranchRemoteInterfaceUrlConnectionAsyncTest.runAllTests()
            println()
            
            // Run Thread.sleep() elimination integration tests
            println("📋 Running Thread.sleep() elimination integration tests...")
            ThreadSleepEliminationIntegrationTest.runAllTests()
            println()
            
        } catch (e: Exception) {
            allTestsPassed = false
            println("❌ Test execution failed: ${e.message}")
            e.printStackTrace()
        }
        
        println("=========================================")
        if (allTestsPassed) {
            println("🎉 ALL NETWORK LAYER TESTS PASSED!")
            println("✅ Thread.sleep() has been successfully eliminated")
            println("✅ Coroutine-based retry mechanism is working")
            println("✅ Exponential backoff with jitter is implemented")
            println("✅ Cancellation support is functional")
            println("✅ Backward compatibility is maintained")
        } else {
            println("💥 SOME TESTS FAILED!")
            println("Please check the error messages above")
        }
        println("=========================================")
    }
    
    /**
     * Main method for running tests directly.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        runAllNetworkTests()
    }
}
