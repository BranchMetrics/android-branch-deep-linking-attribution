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

        // Test different types of requests
        val installRequest = createInstallRequest()
        val openRequest = createOpenRequest()
        val eventRequest = createEventRequest()
        val createUrlRequest = createUrlRequest()

        // Test enqueue with different request types
        queue.enqueue(installRequest)
        Assert.assertEquals("Queue should have 1 item", 1, queue.getSize())
        Assert.assertEquals("First item should be install request", installRequest, queue.peek())

        queue.enqueue(openRequest)
        Assert.assertEquals("Queue should have 2 items", 2, queue.getSize())

        queue.enqueue(eventRequest)
        Assert.assertEquals("Queue should have 3 items", 3, queue.getSize())

        queue.enqueue(createUrlRequest)
        Assert.assertEquals("Queue should have 4 items", 4, queue.getSize())

        // Test MAX_ITEMS limit with mixed request types
        for (i in 0 until 30) {
            queue.enqueue(when (i % 4) {
                0 -> createInstallRequest()
                1 -> createOpenRequest()
                2 -> createEventRequest()
                else -> createUrlRequest()
            })
        }
        Assert.assertTrue("Queue size should not exceed MAX_ITEMS (25)", queue.getSize() <= 25)
    }

    @Test
    fun testRequestPriorities() = runTest {
        // Test that install/open requests get priority
        val urlRequest = createUrlRequest()
        val eventRequest = createEventRequest()
        val installRequest = createInstallRequest()
        
        queue.enqueue(urlRequest)
        queue.enqueue(eventRequest)
        queue.enqueue(installRequest)
        
        // Install request should be moved to front
        Assert.assertEquals("Install request should be first", installRequest, queue.peek())
        
        val openRequest = createOpenRequest()
        queue.enqueue(openRequest)
        
        // Open request should be second after install
        Assert.assertEquals("Open request should be second", openRequest, queue.peekAt(1))
    }

    @Test
    fun testSessionManagement() = runTest {
        // Test both install and open session requests
        Assert.assertNull("No init request in empty queue", adapter.getSelfInitRequest())
        
        val installRequest = createInstallRequest()
        adapter.handleNewRequest(installRequest)
        delay(100)
        Assert.assertNotNull("Should find install request", adapter.getSelfInitRequest())
        
        queue.clear()
        
        val openRequest = createOpenRequest()
        adapter.handleNewRequest(openRequest)
        delay(100)
        Assert.assertNotNull("Should find open request", adapter.getSelfInitRequest())
        
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

    // Helper methods to create different types of requests
    private fun createInstallRequest(): ServerRequestInitSession {
        return object : ServerRequestInitSession(RequestPath.RegisterInstall, JSONObject(), testContext, true) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
        }
    }

    private fun createOpenRequest(): ServerRequestInitSession {
        return object : ServerRequestInitSession(RequestPath.RegisterOpen, JSONObject(), testContext, true) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
        }
    }

    private fun createEventRequest(): ServerRequest {
        return object : ServerRequest(RequestPath.LogCustomEvent, JSONObject().apply { 
            put("event_name", "test_event")
            put("metadata", JSONObject().apply { put("test_key", "test_value") })
        }, testContext) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = false
            override fun clearCallbacks() {}
        }
    }

    private fun createUrlRequest(): ServerRequest {
        return object : ServerRequest(RequestPath.GetURL, JSONObject().apply {
            put("alias", "test_alias")
            put("campaign", "test_campaign")
        }, testContext) {
            override fun onRequestSucceeded(resp: ServerResponse, branch: Branch) {}
            override fun handleFailure(statusCode: Int, error: String) {}
            override fun handleErrors(context: Context): Boolean = false
            override fun isGetRequest(): Boolean = true
            override fun clearCallbacks() {}
        }
    }
} 