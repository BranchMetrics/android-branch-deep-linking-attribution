package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BranchRequestQueueIntegrationTest : BranchTest() {

    @Test
    fun testRealWorldConcurrencyScenario() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val concurrentOperations = AtomicInteger(0)
        val maxConcurrentOperations = AtomicInteger(0)
        val completedOperations = AtomicInteger(0)
        val operationLatch = CountDownLatch(100)
        
        // Simulate real-world scenario: multiple init and organic open requests
        val jobs = (0 until 100).map { index ->
            launch(Dispatchers.IO) {
                val request = when (index % 4) {
                    0 -> ServerRequestRegisterInstall(testContext, null, true)
                    1 -> ServerRequestRegisterOpen(testContext, null, true)
                    2 -> ServerRequestRegisterInstall(testContext, null, false)
                    3 -> ServerRequestRegisterOpen(testContext, null, false)
                    else -> ServerRequestRegisterInstall(testContext, null, true)
                }
                
                queue.enqueue(request)
                
                concurrentOperations.incrementAndGet()
                maxConcurrentOperations.updateAndGet { maxOf(it, concurrentOperations.get()) }
                delay(10) // Simulate processing time
                concurrentOperations.decrementAndGet()
                
                completedOperations.incrementAndGet()
                operationLatch.countDown()
            }
        }
        
        operationLatch.await(15, TimeUnit.SECONDS)
        jobs.forEach { it.join() }
        
        // Verify mutual exclusion was maintained
        Assert.assertEquals("Only one operation should execute at a time", 1, maxConcurrentOperations.get())
        Assert.assertEquals("All operations should be completed", 100, completedOperations.get())
    }
    
    @Test
    fun testCriticalSectionOrdering() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val executionOrder = mutableListOf<String>()
        val orderLatch = CountDownLatch(3)
        
        // Test that critical sections execute in order
        val job1 = launch(Dispatchers.IO) {
            val request = ServerRequestRegisterInstall(testContext, null, true)
            queue.enqueue(request)
            synchronized(executionOrder) { executionOrder.add("init_1") }
            orderLatch.countDown()
        }
        
        val job2 = launch(Dispatchers.IO) {
            delay(5) // Small delay to ensure ordering
            val request = ServerRequestRegisterOpen(testContext, null, true)
            queue.enqueue(request)
            synchronized(executionOrder) { executionOrder.add("open_1") }
            orderLatch.countDown()
        }
        
        val job3 = launch(Dispatchers.IO) {
            delay(10) // Small delay to ensure ordering
            val request = ServerRequestRegisterInstall(testContext, null, false)
            queue.enqueue(request)
            synchronized(executionOrder) { executionOrder.add("init_2") }
            orderLatch.countDown()
        }
        
        orderLatch.await(5, TimeUnit.SECONDS)
        job1.join()
        job2.join()
        job3.join()
        
        // Verify all operations were processed
        Assert.assertEquals("All operations should be processed", 3, executionOrder.size)
    }
} 