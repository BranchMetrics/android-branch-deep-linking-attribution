package io.branch.referral

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Modern Kotlin-based request queue using Coroutines and Channels
 * Replaces the manual queueing system with a more robust, thread-safe solution
 */
class BranchRequestQueue private constructor(private val context: Context) {
    
    // Coroutine scope for managing queue operations
    private val queueScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Channel for queuing requests (unbounded to prevent blocking)
    private val requestChannel = Channel<ServerRequest>(capacity = Channel.UNLIMITED)
    
    // State management
    private val _queueState = MutableStateFlow(QueueState.IDLE)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    private val networkCount = AtomicInteger(0)
    private val isProcessing = AtomicBoolean(false)
    
    // Track active requests and instrumentation data
    private val activeRequests = ConcurrentHashMap<String, ServerRequest>()
    val instrumentationExtraData = ConcurrentHashMap<String, String>()
    
    enum class QueueState {
        IDLE, PROCESSING, PAUSED, SHUTDOWN
    }
    
    companion object {
        @Volatile
        private var INSTANCE: BranchRequestQueue? = null
        
        fun getInstance(context: Context): BranchRequestQueue {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BranchRequestQueue(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // For testing and cleanup
        internal fun shutDown() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    init {
        startProcessing()
    }
    
    /**
     * Enqueue a new request
     */
    suspend fun enqueue(request: ServerRequest) {
        if (_queueState.value == QueueState.SHUTDOWN) {
            BranchLogger.w("Cannot enqueue request - queue is shutdown")
            return
        }
        
        BranchLogger.v("Enqueuing request: $request")
        request.onRequestQueued()
        
        try {
            requestChannel.send(request)
        } catch (e: Exception) {
            BranchLogger.e("Failed to enqueue request: ${e.message}")
            request.handleFailure(BranchError.ERR_OTHER, "Failed to enqueue request")
        }
    }
    
    /**
     * Start processing requests from the channel
     */
    private fun startProcessing() {
        queueScope.launch {
            _queueState.value = QueueState.PROCESSING
            
            try {
                for (request in requestChannel) {
                    if (_queueState.value == QueueState.SHUTDOWN) break
                    
                    processRequest(request)
                }
            } catch (e: Exception) {
                BranchLogger.e("Error in request processing: ${e.message}")
            }
        }
    }
    
    /**
     * Process individual request with proper dispatcher selection
     */
    private suspend fun processRequest(request: ServerRequest) {
        if (!canProcessRequest(request)) {
            // Re-queue the request if it can't be processed yet
            delay(100) // Small delay before retry
            requestChannel.send(request)
            return
        }
        
        val requestId = "${request::class.simpleName}_${System.currentTimeMillis()}"
        activeRequests[requestId] = request
        
        try {
            // Increment network count
            networkCount.incrementAndGet()
            
            when {
                request.isWaitingOnProcessToFinish() -> {
                    BranchLogger.v("Request $request is waiting on processes to finish")
                    // Re-queue after delay
                    delay(50)
                    requestChannel.send(request)
                }
                !hasValidSession(request) -> {
                    BranchLogger.v("Request $request has no valid session")
                    request.handleFailure(BranchError.ERR_NO_SESSION, "Request has no session")
                }
                else -> {
                    executeRequest(request)
                }
            }
        } catch (e: Exception) {
            BranchLogger.e("Error processing request $request: ${e.message}")
            request.handleFailure(BranchError.ERR_OTHER, "Request processing failed: ${e.message}")
        } finally {
            activeRequests.remove(requestId)
            networkCount.decrementAndGet()
        }
    }
    
    /**
     * Execute the actual network request using appropriate dispatcher
     */
    private suspend fun executeRequest(request: ServerRequest) = withContext(Dispatchers.IO) {
        BranchLogger.v("Executing request: $request")
        
        try {
            // Pre-execution on Main thread for UI-related updates
            withContext(Dispatchers.Main) {
                request.onPreExecute()
                request.doFinalUpdateOnMainThread()
            }
            
            // Background processing
            request.doFinalUpdateOnBackgroundThread()
            
            // Check if tracking is disabled
            val branch = Branch.getInstance()
            if (branch.trackingController.isTrackingDisabled && !request.prepareExecuteWithoutTracking()) {
                val response = ServerResponse(request.requestPath, BranchError.ERR_BRANCH_TRACKING_DISABLED, "", "Tracking is disabled")
                handleResponse(request, response)
                return@withContext
            }
            
            // Execute network call
            val branchKey = branch.prefHelper_.branchKey
            val response = if (request.isGetRequest) {
                branch.branchRemoteInterface.make_restful_get(
                    request.requestUrl,
                    request.getParams,
                    request.requestPath,
                    branchKey
                )
            } else {
                branch.branchRemoteInterface.make_restful_post(
                    request.getPostWithInstrumentationValues(instrumentationExtraData),
                    request.requestUrl,
                    request.requestPath,
                    branchKey
                )
            }
            
            // Handle response on Main thread
            withContext(Dispatchers.Main) {
                handleResponse(request, response)
            }
            
        } catch (e: Exception) {
            BranchLogger.e("Network request failed: ${e.message}")
            withContext(Dispatchers.Main) {
                request.handleFailure(BranchError.ERR_OTHER, "Network request failed: ${e.message}")
            }
        }
    }
    
    /**
     * Handle network response
     */
    private fun handleResponse(request: ServerRequest, response: ServerResponse?) {
        if (response == null) {
            request.handleFailure(BranchError.ERR_OTHER, "Null response")
            return
        }
        
        when (response.statusCode) {
            200 -> {
                try {
                    request.onRequestSucceeded(response, Branch.getInstance())
                } catch (e: Exception) {
                    BranchLogger.e("Error in onRequestSucceeded: ${e.message}")
                    request.handleFailure(BranchError.ERR_OTHER, "Success handler failed")
                }
            }
            else -> {
                request.handleFailure(response.statusCode, response.failReason ?: "Request failed")
            }
        }
    }
    
    /**
     * Check if request can be processed
     */
    private fun canProcessRequest(request: ServerRequest): Boolean {
        return when {
            request.isWaitingOnProcessToFinish() -> false
            !hasValidSession(request) && requestNeedsSession(request) -> false
            else -> true
        }
    }
    
    /**
     * Check if request needs a session
     */
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        return when (request) {
            is ServerRequestInitSession -> false
            is ServerRequestCreateUrl -> false
            else -> true
        }
    }
    
    /**
     * Check if valid session exists for request
     */
    private fun hasValidSession(request: ServerRequest): Boolean {
        if (!requestNeedsSession(request)) return true
        
        val branch = Branch.getInstance()
        val hasSession = !branch.prefHelper_.sessionID.equals(PrefHelper.NO_STRING_VALUE)
        val hasDeviceToken = !branch.prefHelper_.randomizedDeviceToken.equals(PrefHelper.NO_STRING_VALUE)
        val hasUser = !branch.prefHelper_.randomizedBundleToken.equals(PrefHelper.NO_STRING_VALUE)
        
        return when (request) {
            is ServerRequestRegisterInstall -> hasSession && hasDeviceToken
            else -> hasSession && hasDeviceToken && hasUser
        }
    }
    
    /**
     * Get current queue size (for compatibility)
     */
    fun getSize(): Int {
        return activeRequests.size
    }
    
    /**
     * Check if queue has user
     */
    fun hasUser(): Boolean {
        return !Branch.getInstance().prefHelper_.randomizedBundleToken.equals(PrefHelper.NO_STRING_VALUE)
    }
    
    /**
     * Add instrumentation data
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        instrumentationExtraData[key] = value
    }
    
    /**
     * Clear all pending requests
     */
    suspend fun clear() {
        activeRequests.clear()
        // Drain the channel
        while (!requestChannel.isEmpty) {
            requestChannel.tryReceive()
        }
        BranchLogger.v("Queue cleared")
    }
    
    /**
     * Pause queue processing
     */
    fun pause() {
        _queueState.value = QueueState.PAUSED
    }
    
    /**
     * Resume queue processing
     */
    fun resume() {
        if (_queueState.value == QueueState.PAUSED) {
            _queueState.value = QueueState.PROCESSING
        }
    }
    
    /**
     * Shutdown the queue
     */
    private fun shutdown() {
        _queueState.value = QueueState.SHUTDOWN
        requestChannel.close()
        queueScope.cancel("Queue shutdown")
        activeRequests.clear()
        instrumentationExtraData.clear()
    }
    
    /**
     * Print queue state for debugging
     */
    fun printQueue() {
        if (BranchLogger.loggingLevel.level >= BranchLogger.BranchLogLevel.VERBOSE.level) {
            val activeCount = activeRequests.size
            val channelSize = if (requestChannel.isEmpty) 0 else "unknown" // Channel doesn't expose size
            BranchLogger.v("Queue state: ${_queueState.value}, Active requests: $activeCount, Network count: ${networkCount.get()}")
        }
    }
} 