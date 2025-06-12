package io.branch.referral

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.branch.referral.Defines.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BranchMigrationTest : BranchTest() {

    private lateinit var queue: BranchRequestQueue
    private lateinit var adapter: BranchRequestQueueAdapter

    @Before
    fun setup() {
        initBranchInstance()
        queue = BranchRequestQueue.getInstance(testContext)
        adapter = BranchRequestQueueAdapter.getInstance(testContext)
    }

    @Test
    fun testQueueOperations() = runTest {
        // Test queue starts empty
        Assert.assertEquals("Queue should start empty", 0, queue.getSize())
        Assert.assertNull("Peek on empty queue should return null", queue.peek())

        // Create test requests
        val request1 = createTestRequest("test1")
        val request2 = createTestRequest("test2")
        val request3 = createTestRequest("test3")

        // Test enqueue
        queue.enqueue(request1)
        Assert.assertEquals("Queue should have 1 item", 1, queue.getSize())
        Assert.assertEquals("First item should be request1", request1, queue.peek())

        // Test MAX_ITEMS limit
        for (i in 0 until 30) {
            queue.enqueue(createTestRequest("test_$i"))
        }
        Assert.assertTrue("Queue size should not exceed MAX_ITEMS (25)", queue.getSize() <= 25)
    }

    @Test
    fun testAdapterCompatibility() = runTest {
        // Test adapter operations match queue operations
        val request = createTestRequest("test")
        
        adapter.handleNewRequest(request)
        delay(100) // Wait for async operation
        Assert.assertEquals("Adapter and queue sizes should match", adapter.getSize(), queue.getSize())
        Assert.assertEquals("Adapter and queue peek should match", adapter.peek(), queue.peek())
        
        // Test adapter-specific operations
        val frontRequest = createTestRequest("front")
        adapter.insertRequestAtFront(frontRequest)
        Assert.assertEquals("Front request should be first", frontRequest, adapter.peek())
        
        // Test instrumentation data
        val testKey = "test_key"
        val testValue = "test_value"
        adapter.addExtraInstrumentationData(testKey, testValue)
        Assert.assertEquals("Instrumentation data should be accessible", testValue, adapter.instrumentationExtraData_[testKey])
        
        adapter.clear()
        delay(100) // Wait for async operation
        Assert.assertEquals("Queue should be empty after clear", 0, adapter.getSize())
    }

    @Test
    fun testSessionManagement() = runTest {
        // Test session initialization
        Assert.assertNull("No init request in empty queue", adapter.getSelfInitRequest())
        
        val initRequest = object : ServerRequestInitSession(RequestPath.RegisterInstall, JSONObject(), testContext, true) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
            override fun getRequestActionName(): String = ""
        }
        
        adapter.handleNewRequest(initRequest)
        delay(100) // Wait for async operation
        Assert.assertNotNull("Should find init request", adapter.getSelfInitRequest())
        
        // Test session data updates
        adapter.updateAllRequestsInQueue()
        Assert.assertTrue("Should be able to clear init data", adapter.canClearInitData())
        
        adapter.postInitClear()
        Assert.assertFalse("Should not have user after clear", adapter.hasUser())
    }

    @Test
    fun testQueueStateManagement() = runTest {
        // Test queue state transitions
        Assert.assertEquals("Queue should start IDLE", BranchRequestQueue.QueueState.IDLE, queue.queueState.value)
        
        queue.pause()
        Assert.assertEquals("Queue should be PAUSED", BranchRequestQueue.QueueState.PAUSED, queue.queueState.value)
        
        queue.resume()
        Assert.assertEquals("Queue should be PROCESSING", BranchRequestQueue.QueueState.PROCESSING, queue.queueState.value)
    }

    @Test
    fun testErrorHandling() = runTest {
        var errorCaught = false
        val failingRequest = object : ServerRequest(RequestPath.GetURL, JSONObject(), testContext) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {
                errorCaught = true
            }
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
        }
        
        adapter.handleNewRequest(failingRequest)
        delay(100) // Wait for error handling
        Assert.assertTrue("Error should be caught and handled", errorCaught)
    }

    private fun createTestRequest(requestId: String): ServerRequest {
        return object : ServerRequest(RequestPath.GetURL, JSONObject().apply { put("id", requestId) }, testContext) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
        }
    }
} 