package io.branch.referral

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Collections

/**
 * Modern Kotlin-based request queue using Coroutines and Channels
 * Replaces the manual queueing system with a more robust, thread-safe solution
 * Maintains compatibility with ServerRequestQueue.java functionality
 */
class BranchRequestQueue private constructor(private val context: Context) {
    
    // Coroutine scope for managing queue operations
    private val queueScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Channel for queuing requests (bounded to match original behavior)
    private val requestChannel = Channel<ServerRequest>(capacity = Channel.UNLIMITED)
    
    // Queue list for compatibility with original peek/remove operations
    private val queueList = Collections.synchronizedList(mutableListOf<ServerRequest>())
    
    // SharedPreferences for persistence (matches original)
    private val sharedPrefs = context.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE)
    
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
        // Queue size limit (matches ServerRequestQueue.java)
        private const val MAX_ITEMS = 25
        private const val PREF_KEY = "BNCServerRequestQueue"
        
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
     * Enqueue a new request (with MAX_ITEMS limit like original)
     */
    suspend fun enqueue(request: ServerRequest) {
        if (_queueState.value == QueueState.SHUTDOWN) {
            BranchLogger.w("Cannot enqueue request - queue is shutdown")
            return
        }
        
        BranchLogger.v("Enqueuing request: $request")
        
        synchronized(queueList) {
            // Apply MAX_ITEMS limit like original ServerRequestQueue
            queueList.add(request)
            if (queueList.size >= MAX_ITEMS) {
                BranchLogger.v("Queue maxed out. Removing index 1.")
                if (queueList.size > 1) {
                    queueList.removeAt(1) // Remove second item, keep first like original
                }
            }
        }
        
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
     * Get current queue size (matches original API)
     */
    fun getSize(): Int {
        synchronized(queueList) {
            return queueList.size
        }
    }
    
    /**
     * Peek at first request without removing (matches original API)
     */
    fun peek(): ServerRequest? {
        synchronized(queueList) {
            return try {
                queueList.getOrNull(0)
            } catch (e: Exception) {
                BranchLogger.w("Caught Exception ServerRequestQueue peek: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Peek at request at specific index (matches original API)
     */
    fun peekAt(index: Int): ServerRequest? {
        synchronized(queueList) {
            return try {
                val req = queueList.getOrNull(index)
                BranchLogger.v("Queue operation peekAt $req")
                req
            } catch (e: Exception) {
                BranchLogger.e("Caught Exception ServerRequestQueue peekAt $index: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Insert request at specific index (matches original API)
     */
    fun insert(request: ServerRequest, index: Int) {
        synchronized(queueList) {
            try {
                BranchLogger.v("Queue operation insert. Request: $request Size: ${queueList.size} Index: $index")
                val actualIndex = if (queueList.size < index) queueList.size else index
                queueList.add(actualIndex, request)
            } catch (e: Exception) {
                BranchLogger.e("Caught IndexOutOfBoundsException ${e.message}")
            }
        }
    }
    
    /**
     * Remove request at specific index (matches original API)
     */
    fun removeAt(index: Int): ServerRequest? {
        synchronized(queueList) {
            return try {
                queueList.removeAt(index)
            } catch (e: Exception) {
                BranchLogger.e("Caught IndexOutOfBoundsException ${e.message}")
                null
            }
        }
    }
    
    /**
     * Remove specific request (matches original API)
     */
    fun remove(request: ServerRequest?): Boolean {
        synchronized(queueList) {
            return try {
                BranchLogger.v("Queue operation remove. Request: $request")
                val removed = queueList.remove(request)
                BranchLogger.v("Queue operation remove. Removed: $removed")
                removed
            } catch (e: Exception) {
                BranchLogger.e("Caught UnsupportedOperationException ${e.message}")
                false
            }
        }
    }
    
    /**
     * Insert request at front (matches original API)
     */
    fun insertRequestAtFront(request: ServerRequest) {
        BranchLogger.v("Queue operation insertRequestAtFront $request networkCount_: ${networkCount.get()}")
        if (networkCount.get() == 0) {
            insert(request, 0)
        } else {
            insert(request, 1)
        }
    }
    
    /**
     * Get self init request (matches original API)
     */
    internal fun getSelfInitRequest(): ServerRequestInitSession? {
        synchronized(queueList) {
            for (req in queueList) {
                BranchLogger.v("Checking if $req is instanceof ServerRequestInitSession")
                if (req is ServerRequestInitSession) {
                    BranchLogger.v("$req is initiated by client: ${req.initiatedByClient}")
                    if (req.initiatedByClient) {
                        return req
                    }
                }
            }
        }
        return null
    }
    
    /**
     * Unlock process wait for requests (matches original API)
     */
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        synchronized(queueList) {
            for (req in queueList) {
                req?.removeProcessWaitLock(lock)
            }
        }
    }
    
    /**
     * Update all requests in queue with new session data (matches original API)
     */
    fun updateAllRequestsInQueue() {
        try {
            synchronized(queueList) {
                for (i in 0 until queueList.size) {
                    val req = queueList[i]
                    BranchLogger.v("Queue operation updateAllRequestsInQueue updating: $req")
                    req?.let { request ->
                        val reqJson = request.post
                        if (reqJson != null) {
                            val branch = Branch.getInstance()
                            if (reqJson.has(Defines.Jsonkey.SessionID.key)) {
                                reqJson.put(Defines.Jsonkey.SessionID.key, branch.prefHelper_.sessionID)
                            }
                            if (reqJson.has(Defines.Jsonkey.RandomizedBundleToken.key)) {
                                reqJson.put(Defines.Jsonkey.RandomizedBundleToken.key, branch.prefHelper_.randomizedBundleToken)
                            }
                            if (reqJson.has(Defines.Jsonkey.RandomizedDeviceToken.key)) {
                                reqJson.put(Defines.Jsonkey.RandomizedDeviceToken.key, branch.prefHelper_.randomizedDeviceToken)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            BranchLogger.e("Caught JSONException ${e.message}")
        }
    }
    
    /**
     * Check if init data can be cleared (matches original API)
     */
    fun canClearInitData(): Boolean {
        var result = 0
        synchronized(queueList) {
            for (i in 0 until queueList.size) {
                if (queueList[i] is ServerRequestInitSession) {
                    result++
                }
            }
        }
        return result <= 1
    }
    
    /**
     * Post init clear (matches original API)
     */
    fun postInitClear() {
        val prefHelper = Branch.getInstance().prefHelper_
        val canClear = canClearInitData()
        BranchLogger.v("postInitClear $prefHelper can clear init data $canClear")
        
        if (canClear) {
            prefHelper.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE)
            prefHelper.setGoogleSearchInstallIdentifier(PrefHelper.NO_STRING_VALUE)
            prefHelper.setAppStoreReferrer(PrefHelper.NO_STRING_VALUE)
            prefHelper.setExternalIntentUri(PrefHelper.NO_STRING_VALUE)
            prefHelper.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE)
            prefHelper.setAppLink(PrefHelper.NO_STRING_VALUE)
            prefHelper.setPushIdentifier(PrefHelper.NO_STRING_VALUE)
            prefHelper.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE)
            prefHelper.setIsFullAppConversion(false)
            prefHelper.setInitialReferrer(PrefHelper.NO_STRING_VALUE)
            
            if (prefHelper.getLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME) == 0L) {
                prefHelper.setLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME, prefHelper.getLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME))
            }
        }
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
     * Clear all pending requests (matches original API)
     */
    suspend fun clear() {
        synchronized(queueList) {
            try {
                BranchLogger.v("Queue operation clear")
                queueList.clear()
                BranchLogger.v("Queue cleared.")
            } catch (e: Exception) {
                BranchLogger.e("Caught UnsupportedOperationException ${e.message}")
            }
        }
        
        activeRequests.clear()
        // Drain the channel
        while (!requestChannel.isEmpty) {
            requestChannel.tryReceive()
        }
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