package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BranchRequestQueueTest : BranchTest() {

    @Test
    fun testNewQueueCreation() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        Assert.assertNotNull(queue)
        Assert.assertEquals(0, queue.getSize())
    }

    @Test
    fun testQueueStateManagement() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        // Initially should be processing
        val initialState = queue.queueState.value
        Assert.assertTrue("Queue should be in PROCESSING or IDLE state", 
            initialState == BranchRequestQueue.QueueState.PROCESSING || 
            initialState == BranchRequestQueue.QueueState.IDLE)
    }

    @Test
    fun testInstrumentationData() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        queue.addExtraInstrumentationData("test_key", "test_value")
        Assert.assertEquals("test_value", queue.instrumentationExtraData["test_key"])
    }

    @Test 
    fun testQueueClear() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        // Add some instrumentation data
        queue.addExtraInstrumentationData("test_key", "test_value")
        
        // Clear the queue
        queue.clear()
        
        // Verify queue is empty
        Assert.assertEquals(0, queue.getSize())
    }

    @Test
    fun testAdapterCompatibility() = runTest {
        initBranchInstance()
        val adapter = BranchRequestQueueAdapter.getInstance(testContext)
        
        Assert.assertNotNull(adapter)
        Assert.assertEquals(0, adapter.getSize())
        
        // Test that compatibility methods don't crash
        adapter.printQueue()
        adapter.processNextQueueItem("test")
        adapter.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
        adapter.postInitClear()
        
        Assert.assertTrue(adapter.canClearInitData())
    }

    @Test
    fun testAdapterInstrumentationData() = runTest {
        initBranchInstance()
        val adapter = BranchRequestQueueAdapter.getInstance(testContext)
        
        adapter.addExtraInstrumentationData("adapter_key", "adapter_value")
        
        // Verify the data is passed through to the underlying queue
        val queue = BranchRequestQueue.getInstance(testContext)
        Assert.assertEquals("adapter_value", queue.instrumentationExtraData["adapter_key"])
    }

    @Test
    fun testQueuePauseAndResume() = runTest {
        initBranchInstance()
        val queue = BranchRequestQueue.getInstance(testContext)
        
        // Pause the queue
        queue.pause()
        Assert.assertEquals(BranchRequestQueue.QueueState.PAUSED, queue.queueState.value)
        
        // Resume the queue
        queue.resume()
        Assert.assertEquals(BranchRequestQueue.QueueState.PROCESSING, queue.queueState.value)
    }

    @Test
    fun testMultipleQueueInstances() = runTest {
        initBranchInstance()
        val queue1 = BranchRequestQueue.getInstance(testContext)
        val queue2 = BranchRequestQueue.getInstance(testContext)
        
        // Should be the same instance (singleton)
        Assert.assertSame(queue1, queue2)
    }

    @Test
    fun testAdapterSingletonBehavior() = runTest {
        initBranchInstance()
        val adapter1 = BranchRequestQueueAdapter.getInstance(testContext)
        val adapter2 = BranchRequestQueueAdapter.getInstance(testContext)
        
        // Should be the same instance (singleton)
        Assert.assertSame(adapter1, adapter2)
    }
} 