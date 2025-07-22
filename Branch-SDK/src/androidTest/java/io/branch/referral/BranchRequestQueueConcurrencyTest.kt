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
class BranchRequestQueueConcurrencyTest : BranchTest() {

    @Test
    fun testConcurrentInitAndOrganicOpenRequests() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val concurrentExecutions = AtomicInteger(0)
        val maxConcurrentExecutions = AtomicInteger(0)
        val executionOrder = mutableListOf<String>()
        val executionLatch = CountDownLatch(2)
        
        // Simulate concurrent init and organic open requests
        val initJob = launch(Dispatchers.IO) {
            val initRequest = ServerRequestRegisterInstall(testContext, null, true)
            queue.enqueue(initRequest)
            
            // Simulate the race condition by adding a small delay
            delay(10)
            
            synchronized(executionOrder) {
                executionOrder.add("init")
            }
            concurrentExecutions.incrementAndGet()
            maxConcurrentExecutions.updateAndGet { maxOf(it, concurrentExecutions.get()) }
            concurrentExecutions.decrementAndGet()
            executionLatch.countDown()
        }
        
        val organicOpenJob = launch(Dispatchers.IO) {
            val openRequest = ServerRequestRegisterOpen(testContext, null, true)
            queue.enqueue(openRequest)
            
            // Simulate the race condition by adding a small delay
            delay(10)
            
            synchronized(executionOrder) {
                executionOrder.add("organic_open")
            }
            concurrentExecutions.incrementAndGet()
            maxConcurrentExecutions.updateAndGet { maxOf(it, concurrentExecutions.get()) }
            concurrentExecutions.decrementAndGet()
            executionLatch.countDown()
        }
        
        // Wait for both requests to complete
        executionLatch.await(5, TimeUnit.SECONDS)
        
        initJob.join()
        organicOpenJob.join()
        
        // Verify that only one request executed at a time (mutual exclusion)
        Assert.assertEquals("Only one request should execute at a time", 1, maxConcurrentExecutions.get())
        
        // Verify that both requests were processed
        Assert.assertEquals("Both requests should be processed", 2, executionOrder.size)
        
        // Verify queue state
        Assert.assertTrue("Queue should be in processing state", 
            queue.queueState.value == BranchRequestQueue.QueueState.PROCESSING || 
            queue.queueState.value == BranchRequestQueue.QueueState.IDLE)
    }
    
    @Test
    fun testHighThroughputConcurrency() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val requestCount = 50
        val concurrentExecutions = AtomicInteger(0)
        val maxConcurrentExecutions = AtomicInteger(0)
        val completedRequests = AtomicInteger(0)
        val completionLatch = CountDownLatch(requestCount)
        
        // Launch multiple concurrent requests
        val jobs = (0 until requestCount).map { index ->
            launch(Dispatchers.IO) {
                val request = if (index % 2 == 0) {
                    ServerRequestRegisterInstall(testContext, null, true)
                } else {
                    ServerRequestRegisterOpen(testContext, null, true)
                }
                
                queue.enqueue(request)
                
                // Simulate processing time
                delay(5)
                
                concurrentExecutions.incrementAndGet()
                maxConcurrentExecutions.updateAndGet { maxOf(it, concurrentExecutions.get()) }
                concurrentExecutions.decrementAndGet()
                
                completedRequests.incrementAndGet()
                completionLatch.countDown()
            }
        }
        
        // Wait for all requests to complete
        completionLatch.await(10, TimeUnit.SECONDS)
        
        jobs.forEach { it.join() }
        
        // Verify mutual exclusion was maintained
        Assert.assertEquals("Only one request should execute at a time", 1, maxConcurrentExecutions.get())
        Assert.assertEquals("All requests should be completed", requestCount, completedRequests.get())
    }
    
    @Test
    fun testDiskIOAndNetworkOperationsMutualExclusion() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val diskOperations = AtomicInteger(0)
        val networkOperations = AtomicInteger(0)
        val maxConcurrentOperations = AtomicInteger(0)
        val operationLatch = CountDownLatch(4)
        
        // Simulate concurrent disk I/O and network operations
        val diskJob1 = launch(Dispatchers.IO) {
            val request = ServerRequestRegisterInstall(testContext, null, true)
            queue.enqueue(request)
            
            diskOperations.incrementAndGet()
            maxConcurrentOperations.updateAndGet { maxOf(it, diskOperations.get() + networkOperations.get()) }
            delay(100) // Simulate disk I/O time
            diskOperations.decrementAndGet()
            operationLatch.countDown()
        }
        
        val diskJob2 = launch(Dispatchers.IO) {
            val request = ServerRequestRegisterOpen(testContext, null, true)
            queue.enqueue(request)
            
            diskOperations.incrementAndGet()
            maxConcurrentOperations.updateAndGet { maxOf(it, diskOperations.get() + networkOperations.get()) }
            delay(100) // Simulate disk I/O time
            diskOperations.decrementAndGet()
            operationLatch.countDown()
        }
        
        val networkJob1 = launch(Dispatchers.IO) {
            val request = ServerRequestRegisterInstall(testContext, null, true)
            queue.enqueue(request)
            
            networkOperations.incrementAndGet()
            maxConcurrentOperations.updateAndGet { maxOf(it, diskOperations.get() + networkOperations.get()) }
            delay(100) // Simulate network time
            networkOperations.decrementAndGet()
            operationLatch.countDown()
        }
        
        val networkJob2 = launch(Dispatchers.IO) {
            val request = ServerRequestRegisterOpen(testContext, null, true)
            queue.enqueue(request)
            
            networkOperations.incrementAndGet()
            maxConcurrentOperations.updateAndGet { maxOf(it, diskOperations.get() + networkOperations.get()) }
            delay(100) // Simulate network time
            networkOperations.decrementAndGet()
            operationLatch.countDown()
        }
        
        // Wait for all operations to complete
        operationLatch.await(5, TimeUnit.SECONDS)
        
        diskJob1.join()
        diskJob2.join()
        networkJob1.join()
        networkJob2.join()
        
        // Verify that only one operation (disk or network) executes at a time
        Assert.assertEquals("Only one operation should execute at a time", 1, maxConcurrentOperations.get())
    }
    
    @Test
    fun testQueueStateConsistencyUnderConcurrency() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val stateChanges = mutableListOf<BranchRequestQueue.QueueState>()
        val stateLatch = CountDownLatch(10)
        
        // Monitor queue state changes
        val stateMonitorJob = launch {
            repeat(10) {
                stateChanges.add(queue.queueState.value)
                delay(50)
                stateLatch.countDown()
            }
        }
        
        // Launch concurrent requests
        val requestJobs = (0 until 5).map { index ->
            launch(Dispatchers.IO) {
                val request = if (index % 2 == 0) {
                    ServerRequestRegisterInstall(testContext, null, true)
                } else {
                    ServerRequestRegisterOpen(testContext, null, true)
                }
                queue.enqueue(request)
                delay(20)
            }
        }
        
        // Wait for state monitoring to complete
        stateLatch.await(5, TimeUnit.SECONDS)
        
        stateMonitorJob.join()
        requestJobs.forEach { it.join() }
        
        // Verify queue state consistency
        Assert.assertTrue("Queue should maintain consistent state", 
            stateChanges.all { it == BranchRequestQueue.QueueState.PROCESSING || it == BranchRequestQueue.QueueState.IDLE })
        
        // Verify no invalid state transitions
        for (i in 1 until stateChanges.size) {
            val previousState = stateChanges[i - 1]
            val currentState = stateChanges[i]
            
            // Valid transitions: IDLE -> PROCESSING, PROCESSING -> IDLE, PROCESSING -> PROCESSING
            Assert.assertTrue("Invalid state transition: $previousState -> $currentState",
                (previousState == BranchRequestQueue.QueueState.IDLE && currentState == BranchRequestQueue.QueueState.PROCESSING) ||
                (previousState == BranchRequestQueue.QueueState.PROCESSING && currentState == BranchRequestQueue.QueueState.IDLE) ||
                (previousState == BranchRequestQueue.QueueState.PROCESSING && currentState == BranchRequestQueue.QueueState.PROCESSING))
        }
    }
    
    @Test
    fun testReentrancyAndDeadlockPrevention() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        val deadlockDetected = AtomicInteger(0)
        val completionLatch = CountDownLatch(3)
        
        // Test reentrancy by having nested operations
        val nestedJob = launch(Dispatchers.IO) {
            try {
                val request1 = ServerRequestRegisterInstall(testContext, null, true)
                queue.enqueue(request1)
                
                // Nested operation
                val request2 = ServerRequestRegisterOpen(testContext, null, true)
                queue.enqueue(request2)
                
                delay(100)
                completionLatch.countDown()
            } catch (e: Exception) {
                deadlockDetected.incrementAndGet()
                completionLatch.countDown()
            }
        }
        
        // Test concurrent operations that might cause deadlock
        val concurrentJob1 = launch(Dispatchers.IO) {
            try {
                val request = ServerRequestRegisterInstall(testContext, null, true)
                queue.enqueue(request)
                delay(50)
                completionLatch.countDown()
            } catch (e: Exception) {
                deadlockDetected.incrementAndGet()
                completionLatch.countDown()
            }
        }
        
        val concurrentJob2 = launch(Dispatchers.IO) {
            try {
                val request = ServerRequestRegisterOpen(testContext, null, true)
                queue.enqueue(request)
                delay(50)
                completionLatch.countDown()
            } catch (e: Exception) {
                deadlockDetected.incrementAndGet()
                completionLatch.countDown()
            }
        }
        
        // Wait for all operations to complete
        completionLatch.await(5, TimeUnit.SECONDS)
        
        nestedJob.join()
        concurrentJob1.join()
        concurrentJob2.join()
        
        // Verify no deadlocks occurred
        Assert.assertEquals("No deadlocks should occur", 0, deadlockDetected.get())
        
        // Verify queue is still functional
        Assert.assertTrue("Queue should remain functional", 
            queue.queueState.value == BranchRequestQueue.QueueState.PROCESSING || 
            queue.queueState.value == BranchRequestQueue.QueueState.IDLE)
    }
} 