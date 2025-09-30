package io.branch.referral

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Request retry tracking information
 * Follows SRP - single responsibility for tracking retry data
 */
private data class RequestRetryInfo(
    val requestId: String,
    val firstAttemptTime: Long = System.currentTimeMillis(),
    var retryCount: Int = 0,
    var lastAttemptTime: Long = System.currentTimeMillis(),
    var firstWaitLockTime: Long = 0L
) {
    fun hasExceededRetryLimit(maxRetries: Int): Boolean = retryCount >= maxRetries
    
    fun hasExceededTimeout(timeoutMs: Long): Boolean = 
        (System.currentTimeMillis() - firstAttemptTime) > timeoutMs
        
    fun hasExceededWaitLockTimeout(timeoutMs: Long): Boolean = 
        firstWaitLockTime > 0 && (System.currentTimeMillis() - firstWaitLockTime) > timeoutMs
    
    fun incrementRetry() {
        retryCount++
        lastAttemptTime = System.currentTimeMillis()
    }
    
    fun markFirstWaitLock() {
        if (firstWaitLockTime == 0L) {
            firstWaitLockTime = System.currentTimeMillis()
        }
    }
}

/**
 * Modern Kotlin-based request queue using Coroutines and Channels
 * Replaces the manual queueing system with a more robust, thread-safe solution
 * Maintains compatibility with ServerRequestQueue.java functionality
 */
class BranchRequestQueue private constructor(private val context: Context) {
    
    companion object {
        // Queue size limit (matches ServerRequestQueue.java)
        private const val MAX_ITEMS = 25
        private const val PREF_KEY = "BNCServerRequestQueue"
        
        // Retry mechanism constants - prevents infinite loops
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val REQUEST_TIMEOUT_MS = 30_000L // 30 seconds
        private const val RETRY_DELAY_MS = 100L
        
        @Volatile
        private var INSTANCE: WeakReference<BranchRequestQueue>? = null
        
        @JvmStatic
        fun getInstance(context: Context): BranchRequestQueue {
            // Check if we have a valid instance
            INSTANCE?.get()?.let { return it }
            
            // Create new instance with proper synchronization
            return synchronized(this) {
                // Double-check after acquiring lock
                INSTANCE?.get() ?: run {
                    val newInstance = BranchRequestQueue(context.applicationContext)
                    INSTANCE = WeakReference(newInstance)
                    BranchLogger.d("DEBUG: BranchRequestQueue instance created")
                    newInstance
                }
            }
        }
        
        @JvmStatic
        fun shutDown() {
            BranchLogger.d("DEBUG: BranchRequestQueue.shutDown called")
            INSTANCE?.get()?.let { instance ->
                instance.shutdown()
                INSTANCE = null
            }
            BranchLogger.d("DEBUG: BranchRequestQueue.shutDown completed")
        }
    }
    
    // Coroutine scope for managing queue operations
    private val queueScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Single source of truth - only use queueList, channel is just for processing trigger
    private val queueList = Collections.synchronizedList(mutableListOf<ServerRequest>())
    private val processingTrigger = Channel<Unit>(capacity = Channel.UNLIMITED)
    
    // SharedPreferences for persistence (matches original)
    private val sharedPrefs = context.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE)
    
    // State management
    private val _queueState = MutableStateFlow(QueueState.IDLE)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    private val networkCount = AtomicInteger(0)
    private val isProcessing = AtomicBoolean(false)
    
    // Track active requests and instrumentation data
    private val activeRequests = ConcurrentHashMap<String, ServerRequest>()
    val instrumentationExtraData: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    
    // Track retry information for requests - prevents infinite loops
    private val requestRetryInfo = ConcurrentHashMap<String, RequestRetryInfo>()
    
    enum class QueueState {
        IDLE, PROCESSING, PAUSED, SHUTDOWN
    }
    
    init {
        // Initialize the queue but don't start processing yet
        // Processing will be started explicitly when the SDK is ready
        BranchLogger.d("DEBUG: BranchRequestQueue constructor called")
        BranchLogger.d("DEBUG: BranchRequestQueue constructor completed")
    }
    
    /**
     * Initialize the queue processing - must be called before any requests are processed
     */
    fun initialize() {
        BranchLogger.v("Initializing BranchRequestQueue with coroutines")
        BranchLogger.d("DEBUG: BranchRequestQueue.initialize called")
        startProcessing()
    }
    
    /**
     * Check if the queue is ready to process requests
     */
    fun isReady(): Boolean {
        return _queueState.value == QueueState.PROCESSING
    }
    
    /**
     * Enqueue a new request - SYNCHRONOUS method for compatibility
     * Follows SRP - single responsibility for adding requests to queue
     */
    fun enqueue(request: ServerRequest) {
        BranchLogger.d("DEBUG: BranchRequestQueue.enqueue called for: ${request::class.simpleName}")
        
        if (_queueState.value == QueueState.SHUTDOWN) {
            BranchLogger.w("Cannot enqueue request - queue is shutdown")
            return
        }
        
        // Ensure queue is ready to process requests
        if (_queueState.value == QueueState.IDLE) {
            BranchLogger.v("Queue not initialized, initializing now")
            BranchLogger.d("DEBUG: Queue was IDLE during enqueue, initializing")
            initialize()
        }
        
        BranchLogger.v("Enqueuing request: $request")
        BranchLogger.d("DEBUG: Adding request to queue list")
        
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
        
        // Trigger processing without blocking
        queueScope.launch {
            try {
                BranchLogger.d("DEBUG: Triggering processing for: ${request::class.simpleName}")
                processingTrigger.send(Unit)
            } catch (e: Exception) {
                BranchLogger.e("Failed to trigger processing: ${e.message}")
                request.handleFailure(BranchError.ERR_OTHER, "Failed to trigger processing")
            }
        }
    }
    
    /**
     * Start processing requests from the queue
     * Follows SRP - single responsibility for queue processing coordination
     */
    private fun startProcessing() {
        BranchLogger.d("DEBUG: BranchRequestQueue.startProcessing called")
        queueScope.launch {
            _queueState.value = QueueState.PROCESSING
            BranchLogger.d("DEBUG: Queue state set to PROCESSING")
            
            try {
                for (trigger in processingTrigger) {
                    if (_queueState.value == QueueState.SHUTDOWN) break
                    
                    processNextRequest()
                }
            } catch (e: Exception) {
                BranchLogger.e("Error in request processing: ${e.message}")
            }
        }
    }
    
    /**
     * Process the next available request from queue
     * Follows SRP - single responsibility for processing next request
     */
    private suspend fun processNextRequest() {
        val request = synchronized(queueList) {
            queueList.firstOrNull()
        }
        
        if (request == null) {
            BranchLogger.d("DEBUG: No requests in queue to process")
            return
        }
        
        BranchLogger.d("DEBUG: BranchRequestQueue.processNextRequest called for: ${request::class.simpleName}")
        
        // Enhanced debugging for init session requests
        if (request is ServerRequestInitSession) {
            val requestType = when (request) {
                is ServerRequestRegisterInstall -> "RegisterInstall"
                is ServerRequestRegisterOpen -> "RegisterOpen"
                else -> "InitSession"
            }
            BranchLogger.d("DEBUG: Processing $requestType request - this is a session initialization request")
        }
        
        val requestId = generateRequestId(request)
        
        if (!canProcessRequest(request)) {
            if (request.isWaitingOnProcessToFinish) {
                val waitLocks = request.printWaitLocks()
                BranchLogger.d("DEBUG: Request cannot be processed - waiting on locks: $waitLocks")
            }
            handleRequestCannotBeProcessed(request, requestId)
            return
        }
        
        // Remove from queue since we're processing it
        synchronized(queueList) {
            queueList.remove(request)
        }
        
        // Clear retry info for successful processing attempts
        requestRetryInfo.remove(requestId)
        activeRequests[requestId] = request
        
        try {
            // Increment network count
            networkCount.incrementAndGet()
            BranchLogger.d("DEBUG: Processing request: ${request::class.simpleName}, network count: ${networkCount.get()}")
            
            when {
                request.isWaitingOnProcessToFinish -> {
                    val waitLocks = request.printWaitLocks()
                    BranchLogger.v("Request $request is waiting on processes to finish")
                    BranchLogger.d("DEBUG: Request is waiting on processes to finish, active locks: $waitLocks, re-queuing")
                    BranchLogger.w("WAIT_LOCK_DEBUG: Request ${request::class.simpleName} stuck with locks: $waitLocks")
                    // Re-queue after delay - add back to front of queue
                    delay(50)
                    synchronized(queueList) {
                        queueList.add(0, request)
                    }
                    processingTrigger.send(Unit)
                }
                !hasValidSession(request) -> {
                    BranchLogger.v("Request $request has no valid session")
                    BranchLogger.d("DEBUG: Request has no valid session")
                    
                    // Check if there are any initialization requests in the queue
                    val hasInitRequest = synchronized(queueList) {
                        queueList.any { it is ServerRequestInitSession }
                    }
                    
                    if (!hasInitRequest && request !is ServerRequestInitSession) {
                        BranchLogger.d("DEBUG: No init request in queue and no valid session - request will wait for session initialization")
                        // Add back to queue with delay and trigger auto-initialization if needed
                        delay(100)
                        synchronized(queueList) {
                            queueList.add(0, request)
                        }
                        processingTrigger.send(Unit)
                    } else {
                        BranchLogger.d("DEBUG: Handling failure for request without session")
                        request.handleFailure(BranchError.ERR_NO_SESSION, "Request has no session")
                    }
                }
                else -> {
                    BranchLogger.d("DEBUG: Executing request: ${request::class.simpleName}")
                    if (request is ServerRequestInitSession) {
                        BranchLogger.d("DEBUG: *** EXECUTING SESSION INITIALIZATION REQUEST: ${request::class.simpleName} ***")
                    }
                    executeRequest(request)
                }
            }
        } catch (e: Exception) {
            BranchLogger.e("Error processing request $request: ${e.message}")
            request.handleFailure(BranchError.ERR_OTHER, "Request processing failed: ${e.message}")
        } finally {
            activeRequests.remove(requestId)
            networkCount.decrementAndGet()
            BranchLogger.d("DEBUG: Request processing completed, network count: ${networkCount.get()}")
        }
    }
    
    /**
     * Generate consistent request ID for tracking purposes
     * Uses object identity to ensure same request always gets same ID
     * Follows SRP - single responsibility for ID generation
     */
    private fun generateRequestId(request: ServerRequest): String {
        return "${request::class.simpleName}_${System.identityHashCode(request)}"
    }
    
    /**
     * Handle requests that cannot be processed due to missing session or other conditions
     * Implements retry mechanism with limits to prevent infinite loops
     * Follows SRP - single responsibility for retry logic
     */
    private suspend fun handleRequestCannotBeProcessed(request: ServerRequest, requestId: String) {
        val retryInfo = requestRetryInfo.getOrPut(requestId) { 
            RequestRetryInfo(requestId) 
        }
        
        // Check if request has exceeded limits
        if (shouldFailRequest(retryInfo)) {
            handleRequestFailureWithCleanup(request, requestId, retryInfo)
            // Remove from queue since it failed
            synchronized(queueList) {
                queueList.remove(request)
            }
            return
        }
        
        // Mark first wait lock time for timeout tracking
        if (request.isWaitingOnProcessToFinish) {
            retryInfo.markFirstWaitLock()
        }
        
        // Check for stuck locks and try to resolve them
        if (request.isWaitingOnProcessToFinish) {
            val waitLocks = request.printWaitLocks()
            
            // Check for timeout-based stuck locks (10 seconds)
            if (retryInfo.hasExceededWaitLockTimeout(10_000L)) {
                BranchLogger.w("STUCK_LOCK_DETECTION: Locks have been active for >10s, attempting resolution: $waitLocks")
                tryResolveStuckLocks(request, waitLocks)
            }
            // Check for SDK_INIT_WAIT_LOCK when session is already valid (immediate resolution)
            else if (waitLocks.contains("SDK_INIT_WAIT_LOCK") && isSessionValidForRequest(request)) {
                BranchLogger.w("STUCK_LOCK_DETECTION: SDK_INIT_WAIT_LOCK detected but session is valid, attempting immediate resolution")
                tryResolveStuckSdkInitLock(request)
            }
            // Check for retry-based stuck locks
            else if (retryInfo.retryCount >= 3 && waitLocks.contains("USER_AGENT_STRING_LOCK")) {
                BranchLogger.w("STUCK_LOCK_DETECTION: USER_AGENT_STRING_LOCK detected as stuck after ${retryInfo.retryCount} retries, attempting to resolve")
                tryResolveStuckUserAgentLock(request)
            }
        }
        
        // Increment retry count and attempt requeue
        retryInfo.incrementRetry()
        
        BranchLogger.d("DEBUG: Request cannot be processed yet, retry ${retryInfo.retryCount}/$MAX_RETRY_ATTEMPTS, re-queuing after delay")
        delay(RETRY_DELAY_MS)
        
        // Request stays in queue (don't remove), just trigger processing again
        processingTrigger.send(Unit)
    }
    
    /**
     * Determine if request should fail based on retry limits and timeout
     * Follows SRP - single responsibility for failure criteria evaluation
     */
    private fun shouldFailRequest(retryInfo: RequestRetryInfo): Boolean {
        return retryInfo.hasExceededRetryLimit(MAX_RETRY_ATTEMPTS) || 
               retryInfo.hasExceededTimeout(REQUEST_TIMEOUT_MS)
    }
    
    /**
     * Handle request failure with proper cleanup
     * Follows SRP - single responsibility for failure handling and cleanup
     */
    private fun handleRequestFailureWithCleanup(
        request: ServerRequest, 
        requestId: String, 
        retryInfo: RequestRetryInfo
    ) {
        val errorMessage = when {
            retryInfo.hasExceededRetryLimit(MAX_RETRY_ATTEMPTS) -> 
                "Request exceeded maximum retry attempts (${MAX_RETRY_ATTEMPTS})"
            retryInfo.hasExceededTimeout(REQUEST_TIMEOUT_MS) -> 
                "Request exceeded timeout (${REQUEST_TIMEOUT_MS}ms)"
            else -> "Request failed unknown reason"
        }
        
        BranchLogger.d("DEBUG: $errorMessage for request: ${request::class.simpleName}")
        
        // Clean up retry tracking
        requestRetryInfo.remove(requestId)
        
        // Fail the request
        request.handleFailure(BranchError.ERR_NO_SESSION, errorMessage)
    }
    
    /**
     * Execute the actual network request using appropriate dispatcher
     */
    private suspend fun executeRequest(request: ServerRequest) = withContext(Dispatchers.IO) {
        BranchLogger.v("Executing request: $request")
        BranchLogger.d("DEBUG: BranchRequestQueue.executeRequest called for: ${request::class.simpleName}")
        
        try {
            // Pre-execution on Main thread for UI-related updates
            withContext(Dispatchers.Main) {
                BranchLogger.d("DEBUG: Executing onPreExecute and doFinalUpdateOnMainThread")
                request.onPreExecute()
                request.doFinalUpdateOnMainThread()
            }
            
            // Background processing
            BranchLogger.d("DEBUG: Executing doFinalUpdateOnBackgroundThread")
            request.doFinalUpdateOnBackgroundThread()


            
            // Check if tracking is disabled
            val branch = Branch.getInstance()
            if (branch.trackingController.isTrackingDisabled && !request.prepareExecuteWithoutTracking()) {
                val response = ServerResponse(request.requestPath, BranchError.ERR_BRANCH_TRACKING_DISABLED, "", "Tracking is disabled")
                BranchLogger.d("DEBUG: Tracking is disabled, handling response")
                handleResponse(request, response)
                return@withContext
            }

            // TODO: Handle enqueuing setIdentity & Logout more elegantly
            if(request is QueueOperationSetIdentity || request is QueueOperationLogout) {
                val response = ServerResponse("", 200, "", "")
                withContext(Dispatchers.Main) {
                    BranchLogger.d("DEBUG: Handling response on main thread")
                    handleResponse(request, response)
                }
            }
            // Execute network call
            else {
                val branchKey = branch.prefHelper_.branchKey
                BranchLogger.d("DEBUG: Executing network call with branch key: $branchKey")
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
                    BranchLogger.d("DEBUG: Handling response on main thread")
                    handleResponse(request, response)
                }
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
        BranchLogger.d("DEBUG: BranchRequestQueue.handleResponse called for: ${request::class.simpleName}")
        
        if (response == null) {
            BranchLogger.d("DEBUG: Response is null, handling failure")
            request.handleFailure(BranchError.ERR_OTHER, "Null response")
            return
        }
        
        BranchLogger.d("DEBUG: Response status code: ${response.statusCode}")
        
        when (response.statusCode) {
            200 -> {
                try {
                    BranchLogger.d("DEBUG: Response successful, calling onRequestSucceeded")
                    
                    // Enhanced debugging for init session requests
                    if (request is ServerRequestInitSession) {
                        val requestType = when (request) {
                            is ServerRequestRegisterInstall -> "RegisterInstall"
                            is ServerRequestRegisterOpen -> "RegisterOpen"
                            else -> "InitSession"
                        }
                        BranchLogger.d("DEBUG: *** SUCCESS: $requestType request completed successfully ***")
                    }
                    
                    // Process ServerRequestInitSession response data before calling onRequestSucceeded
                    if (request is ServerRequestInitSession) {
                        processInitSessionResponse(request, response)
                    }
                    
                    request.onRequestSucceeded(response, Branch.getInstance())
                    
                    // Additional logging after successful completion
                    if (request is ServerRequestInitSession) {
                        try {
                            val legacyState = Branch.getInstance().initState_
                            val hasUser = Branch.getInstance().prefHelper_.getRandomizedBundleToken() != PrefHelper.NO_STRING_VALUE
                            BranchLogger.d("DEBUG: After $request completion - LegacyState: $legacyState, hasUser: $hasUser")
                        } catch (e: Exception) {
                            BranchLogger.d("DEBUG: Could not access session state after request completion: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    BranchLogger.e("Error in onRequestSucceeded: ${e.message}")
                    request.handleFailure(BranchError.ERR_OTHER, "Success handler failed")
                }
            }
            else -> {
                BranchLogger.d("DEBUG: Response failed with status: ${response.statusCode}")
                request.handleFailure(response.statusCode, response.failReason ?: "Request failed")
            }
        }
    }
    
    /**
     * Check if session is valid for the given request
     */
    private fun isSessionValidForRequest(request: ServerRequest): Boolean {
        val branch = Branch.getInstance()
        val hasSession = !branch.prefHelper_.sessionID.equals(PrefHelper.NO_STRING_VALUE)
        val hasDeviceToken = !branch.prefHelper_.getRandomizedDeviceToken().equals(PrefHelper.NO_STRING_VALUE)
        val hasUser = !branch.prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE)
        val sessionInitialized = branch.initState_ is BranchSessionState.Initialized
        val canPerformOperations = branch.canPerformOperations()
        
        return (sessionInitialized || canPerformOperations) && hasSession && hasDeviceToken && 
               (request !is ServerRequestRegisterInstall || hasUser)
    }
    
    /**
     * Try to resolve multiple types of stuck locks
     */
    private fun tryResolveStuckLocks(request: ServerRequest, waitLocks: String) {
        BranchLogger.w("STUCK_LOCK_RESOLUTION: Attempting to resolve stuck locks: $waitLocks")
        
        if (waitLocks.contains("USER_AGENT_STRING_LOCK")) {
            tryResolveStuckUserAgentLock(request)
        }
        
        // Add resolution for other common stuck locks
        if (waitLocks.contains("SDK_INIT_WAIT_LOCK")) {
            BranchLogger.w("STUCK_LOCK_RESOLUTION: Attempting to resolve stuck SDK_INIT_WAIT_LOCK")
            tryResolveStuckSdkInitLock(request)
        }
        
        if (waitLocks.contains("GAID_FETCH_WAIT_LOCK")) {
            BranchLogger.w("STUCK_LOCK_RESOLUTION: Forcing removal of stuck GAID_FETCH_WAIT_LOCK")
            request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK)
        }
        
        if (waitLocks.contains("INSTALL_REFERRER_FETCH_WAIT_LOCK")) {
            BranchLogger.w("STUCK_LOCK_RESOLUTION: Forcing removal of stuck INSTALL_REFERRER_FETCH_WAIT_LOCK")
            request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK)
        }
    }
    
    /**
     * Try to resolve a stuck USER_AGENT_STRING_LOCK
     * This can happen when user agent fetch fails or takes too long
     */
    private fun tryResolveStuckUserAgentLock(request: ServerRequest) {
        try {
            // Check if user agent is now available
            if (!android.text.TextUtils.isEmpty(Branch._userAgentString)) {
                BranchLogger.d("DEBUG: User agent is now available: ${Branch._userAgentString}, removing stuck lock")
                request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK)
                return
            }
            
            // If user agent is still empty after retries, force unlock to prevent infinite retry
            BranchLogger.w("STUCK_LOCK_RESOLUTION: Forcing removal of USER_AGENT_STRING_LOCK to unblock request processing")
            request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK)
            
            // Set a fallback user agent to prevent future issues
            if (android.text.TextUtils.isEmpty(Branch._userAgentString)) {
                Branch._userAgentString = "Branch-Android-SDK-Fallback"
                BranchLogger.d("DEBUG: Set fallback user agent to prevent future locks")
            }
            
        } catch (e: Exception) {
            BranchLogger.e("Error resolving stuck USER_AGENT_STRING_LOCK: ${e.message}")
            // Force unlock even if there's an error
            try {
                request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK)
            } catch (ex: Exception) {
                BranchLogger.e("Failed to force unlock USER_AGENT_STRING_LOCK: ${ex.message}")
            }
        }
    }
    
    /**
     * Try to resolve a stuck SDK_INIT_WAIT_LOCK
     * This can happen when session is already initialized but lock wasn't removed
     */
    private fun tryResolveStuckSdkInitLock(request: ServerRequest) {
        try {
            val branch = Branch.getInstance()
            
            // Check if session is actually valid now
            val hasSession = !branch.prefHelper_.sessionID.equals(PrefHelper.NO_STRING_VALUE)
            val hasDeviceToken = !branch.prefHelper_.getRandomizedDeviceToken().equals(PrefHelper.NO_STRING_VALUE)
            val hasUser = !branch.prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE)
            val sessionInitialized = branch.initState_ is BranchSessionState.Initialized
            val canPerformOperations = branch.canPerformOperations()
            
            BranchLogger.d("DEBUG: SDK_INIT_WAIT_LOCK resolution check - hasSession: $hasSession, hasDeviceToken: $hasDeviceToken, hasUser: $hasUser, sessionInitialized: $sessionInitialized, canPerformOperations: $canPerformOperations")
            
            // If session is valid but lock is still present, remove it
            if ((sessionInitialized || canPerformOperations) && hasSession && hasDeviceToken) {
                BranchLogger.w("STUCK_LOCK_RESOLUTION: Session is initialized but SDK_INIT_WAIT_LOCK is still present, removing lock")
                request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
                
                // Also call the official unlock method to ensure consistency
                try {
                    branch.unlockSDKInitWaitLock()
                } catch (e: Exception) {
                    BranchLogger.w("Failed to call unlockSDKInitWaitLock: ${e.message}")
                }
            } else {
                BranchLogger.d("DEBUG: Session not ready yet, keeping SDK_INIT_WAIT_LOCK")
            }
            
        } catch (e: Exception) {
            BranchLogger.e("Error resolving stuck SDK_INIT_WAIT_LOCK: ${e.message}")
            // Force unlock if there's an error to prevent infinite retry
            try {
                request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
                BranchLogger.w("STUCK_LOCK_RESOLUTION: Force removed SDK_INIT_WAIT_LOCK due to error")
            } catch (ex: Exception) {
                BranchLogger.e("Failed to force unlock SDK_INIT_WAIT_LOCK: ${ex.message}")
            }
        }
    }
    
    /**
     * Process ServerRequestInitSession response to extract session tokens
     * Matches the logic from ServerRequestQueue.java for compatibility
     */
    private fun processInitSessionResponse(request: ServerRequestInitSession, response: ServerResponse) {
        BranchLogger.d("DEBUG: Processing init session response for: ${request::class.simpleName}")
        
        try {
            val branch = Branch.getInstance()
            if (branch.trackingController.isTrackingDisabled) {
                BranchLogger.d("DEBUG: Tracking is disabled, skipping token processing")
                return
            }
            
            val respJson = response.getObject()
            if (respJson == null) {
                BranchLogger.d("DEBUG: Response JSON is null, skipping token processing")
                return
            }
            
            var updateRequestsInQueue = false
            
            // Process SessionID
            if (respJson.has(Defines.Jsonkey.SessionID.key)) {
                val sessionId = respJson.getString(Defines.Jsonkey.SessionID.key)
                branch.prefHelper_.sessionID = sessionId
                updateRequestsInQueue = true
                BranchLogger.d("DEBUG: Set SessionID: $sessionId")
            }
            
            // Process RandomizedBundleToken - this is what makes hasUser() return true
            if (respJson.has(Defines.Jsonkey.RandomizedBundleToken.key)) {
                val newRandomizedBundleToken = respJson.getString(Defines.Jsonkey.RandomizedBundleToken.key)
                val currentToken = branch.prefHelper_.getRandomizedBundleToken()
                
                if (currentToken != newRandomizedBundleToken) {
                    // On setting a new Randomized Bundle Token clear the link cache
                    branch.linkCache_.clear()
                    branch.prefHelper_.randomizedBundleToken = newRandomizedBundleToken
                    updateRequestsInQueue = true
                    BranchLogger.d("DEBUG: Set RandomizedBundleToken: $newRandomizedBundleToken (was: $currentToken)")
                }
            }
            
            // Process RandomizedDeviceToken
            if (respJson.has(Defines.Jsonkey.RandomizedDeviceToken.key)) {
                val deviceToken = respJson.getString(Defines.Jsonkey.RandomizedDeviceToken.key)
                branch.prefHelper_.randomizedDeviceToken = deviceToken
                updateRequestsInQueue = true
                BranchLogger.d("DEBUG: Set RandomizedDeviceToken: $deviceToken")
            }
            
            if (updateRequestsInQueue) {
                BranchLogger.d("DEBUG: Updating all requests in queue with new tokens")
                updateAllRequestsInQueue()
            }
            
        } catch (e: Exception) {
            BranchLogger.e("Error processing init session response: ${e.message}")
        }
    }
    
    /**
     * Check if request can be processed
     * ServerRequestInitSession types should always be processed to establish session
     * Follows SRP - single responsibility for processing eligibility
     */
    private fun canProcessRequest(request: ServerRequest): Boolean {
        val isWaiting = request.isWaitingOnProcessToFinish
        val needsSession = requestNeedsSession(request)
        val hasValidSession = hasValidSession(request)
        
        // Enhanced logic for ServerRequestInitSession types
        val result = when {
            // Always allow session initialization requests to proceed if not waiting
            request is ServerRequestInitSession -> {
                val canProceed = !isWaiting
                if (isWaiting) {
                    val waitLocks = request.printWaitLocks()
                    BranchLogger.d("DEBUG: ServerRequestInitSession waiting on locks: $waitLocks")
                }
                BranchLogger.d("DEBUG: ServerRequestInitSession can proceed: $canProceed (isWaiting: $isWaiting)")
                canProceed
            }
            // Regular requests need to check wait locks first
            isWaiting -> {
                val waitLocks = request.printWaitLocks()
                BranchLogger.d("DEBUG: Request is waiting on process locks: $waitLocks")
                false
            }
            // Then check session requirements
            needsSession && !hasValidSession -> {
                BranchLogger.d("DEBUG: Request needs session but doesn't have valid session")
                false
            }
            // Default allow
            else -> {
                BranchLogger.d("DEBUG: Request can proceed (default case)")
                true
            }
        }
        
        BranchLogger.d("DEBUG: canProcessRequest - isWaiting: $isWaiting, needsSession: $needsSession, hasValidSession: $hasValidSession, isInitSession: ${request is ServerRequestInitSession}, result: $result")
        return result
    }
    
    /**
     * Check if request needs a session
     */
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        val result = when (request) {
            is ServerRequestInitSession -> false
            is ServerRequestCreateUrl -> false
            is QueueOperationLogout -> false
            is QueueOperationSetIdentity -> false
            else -> true
        }
        BranchLogger.d("DEBUG: requestNeedsSession for ${request::class.simpleName} - result: $result")
        return result
    }
    
    /**
     * Check if valid session exists for request
     */
    private fun hasValidSession(request: ServerRequest): Boolean {
        if (!requestNeedsSession(request)) {
            BranchLogger.d("DEBUG: Request does not need session, returning true")
            return true
        }
        
        val branch = Branch.getInstance()
        val hasSession = !branch.prefHelper_.sessionID.equals(PrefHelper.NO_STRING_VALUE)
        val hasDeviceToken = !branch.prefHelper_.getRandomizedDeviceToken().equals(PrefHelper.NO_STRING_VALUE)
        val hasUser = !branch.prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE)
        
        val result = when (request) {
            is ServerRequestRegisterInstall -> hasSession && hasDeviceToken
            else -> hasSession && hasDeviceToken && hasUser
        }
        
        BranchLogger.d("DEBUG: hasValidSession - hasSession: $hasSession, hasDeviceToken: $hasDeviceToken, hasUser: $hasUser, result: $result")
        return result
    }
    
    /**
     * Get current queue size (matches original API)
     */
    fun getSize(): Int {
        synchronized(queueList) {
            val size = queueList.size
            BranchLogger.d("DEBUG: BranchRequestQueue.getSize called - result: $size")
            return size
        }
    }
    
    /**
     * Peek at first request
     */
    fun peek(): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueue.peek called")
        synchronized(queueList) {
            val result = queueList.firstOrNull()
            BranchLogger.d("DEBUG: Request peek result: ${result?.javaClass?.simpleName}")
            return result
        }
    }
    
    /**
     * Peek at request at specific index
     */
    fun peekAt(index: Int): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueue.peekAt called for index: $index")
        synchronized(queueList) {
            val result = if (index >= 0 && index < queueList.size) {
                queueList[index]
            } else {
                null
            }
            BranchLogger.d("DEBUG: Request peek at index $index result: ${result?.javaClass?.simpleName}")
            return result
        }
    }
    
    /**
     * Insert request at specific index (matches original API)
     */
    fun insert(request: ServerRequest, index: Int) {
        synchronized(queueList) {
            try {
                BranchLogger.v("Queue operation insert. Request: $request Size: ${queueList.size} Index: $index")
                BranchLogger.d("DEBUG: BranchRequestQueue.insert called for: ${request::class.simpleName} at index: $index, current size: ${queueList.size}")
                val actualIndex = if (queueList.size < index) queueList.size else index
                queueList.add(actualIndex, request)
                BranchLogger.d("DEBUG: Request inserted at actual index: $actualIndex, new size: ${queueList.size}")
            } catch (e: Exception) {
                BranchLogger.e("Caught IndexOutOfBoundsException ${e.message}")
            }
        }
    }
    
    /**
     * Remove request at specific index
     */
    fun removeAt(index: Int): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueue.removeAt called for index: $index")
        synchronized(queueList) {
            val result = if (index >= 0 && index < queueList.size) {
                queueList.removeAt(index)
            } else {
                null
            }
            BranchLogger.d("DEBUG: Request removal at index $index result: ${result?.javaClass?.simpleName}")
            return result
        }
    }
    
    /**
     * Remove specific request from queue
     */
    fun remove(request: ServerRequest?): Boolean {
        BranchLogger.d("DEBUG: BranchRequestQueue.remove called for: ${request?.javaClass?.simpleName}")
        synchronized(queueList) {
            val result = queueList.remove(request)
            BranchLogger.d("DEBUG: Request removal result: $result")
            return result
        }
    }
    
    /**
     * Insert request at front (matches original API)
     */
    fun insertRequestAtFront(request: ServerRequest) {
        BranchLogger.v("Queue operation insertRequestAtFront $request networkCount_: ${networkCount.get()}")
        BranchLogger.d("DEBUG: BranchRequestQueue.insertRequestAtFront called for: ${request::class.simpleName}, network count: ${networkCount.get()}")
        
        if (networkCount.get() == 0) {
            insert(request, 0)
            BranchLogger.d("DEBUG: Inserted request at index 0 (network count was 0)")
        } else {
            insert(request, 1)
            BranchLogger.d("DEBUG: Inserted request at index 1 (network count was ${networkCount.get()})")
        }
    }
    
    /**
     * Get self init request (matches original API)
     */
    fun getSelfInitRequest(): ServerRequest? {
        synchronized(queueList) {
            for (req in queueList) {
                BranchLogger.v("Checking if $req is instanceof ServerRequestInitSession")
                if (req is ServerRequestInitSession) {
                    BranchLogger.v("$req is initiated by client: ${req.initiatedByClient}")
                    if (req.initiatedByClient) {
                        BranchLogger.d("DEBUG: Found self init request: ${req::class.simpleName}")
                        return req
                    }
                }
            }
        }
        BranchLogger.d("DEBUG: No self init request found in queue")
        return null
    }
    
    /**
     * Unlock process wait (matches original API)
     */
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        BranchLogger.v("Queue operation unlockProcessWait $lock")
        BranchLogger.d("DEBUG: BranchRequestQueue.unlockProcessWait called for lock: $lock")
        
        synchronized(queueList) {
            for (req in queueList) {
                req.removeProcessWaitLock(lock)
            }
        }
        BranchLogger.d("DEBUG: Process wait lock $lock removed from all requests")
    }
    
    /**
     * Update all requests in queue
     */
    fun updateAllRequestsInQueue() {
        BranchLogger.d("DEBUG: BranchRequestQueue.updateAllRequestsInQueue called")
        synchronized(queueList) {
            for (req in queueList) {
                req.updateEnvironment(context, req.post)
            }
        }
        BranchLogger.d("DEBUG: BranchRequestQueue.updateAllRequestsInQueue completed")
    }
    
    /**
     * Check if init data can be cleared
     */
    fun canClearInitData(): Boolean {
        val result = synchronized(queueList) {
            queueList.none { it is ServerRequestInitSession }
        }
        BranchLogger.d("DEBUG: BranchRequestQueue.canClearInitData called - result: $result")
        return result
    }
    
    /**
     * Clear init data after initialization
     */
    suspend fun postInitClear() {
        BranchLogger.d("DEBUG: BranchRequestQueue.postInitClear called")
        synchronized(queueList) {
            queueList.removeAll { it is ServerRequestInitSession }
        }
        BranchLogger.d("DEBUG: BranchRequestQueue.postInitClear completed")
    }
    
    /**
     * Check if queue has user
     */
    fun hasUser(): Boolean {
        val hasUser = !Branch.getInstance().prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE)
        BranchLogger.d("DEBUG: BranchRequestQueue.hasUser called - result: $hasUser")
        return hasUser
    }
    
    /**
     * Add extra instrumentation data
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        BranchLogger.d("DEBUG: BranchRequestQueue.addExtraInstrumentationData called - key: $key, value: $value")
        instrumentationExtraData[key] = value
        BranchLogger.d("DEBUG: BranchRequestQueue.addExtraInstrumentationData completed")
    }
    
    /**
     * Clear all requests from queue
     * Follows SRP - single responsibility for clearing queue state
     */
    suspend fun clear() {
        BranchLogger.d("DEBUG: BranchRequestQueue.clear called")
        synchronized(queueList) {
            queueList.clear()
        }
        activeRequests.clear()
        requestRetryInfo.clear()
        BranchLogger.d("DEBUG: BranchRequestQueue.clear completed")
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
     * Shutdown the queue and cleanup resources
     * Follows SRP - single responsibility for clean shutdown with proper cleanup
     */
    fun shutdown() {
        BranchLogger.d("DEBUG: BranchRequestQueue.shutdown called")
        _queueState.value = QueueState.SHUTDOWN
        queueScope.cancel()
        
        // Clean up retry tracking maps to prevent memory leaks
        cleanupRetryTrackingMaps()
        
        BranchLogger.d("DEBUG: BranchRequestQueue.shutdown completed")
    }
    
    /**
     * Clean up retry tracking maps to prevent memory leaks
     * Follows SRP - single responsibility for memory cleanup
     */
    private fun cleanupRetryTrackingMaps() {
        try {
            requestRetryInfo.clear()
            activeRequests.clear()
            BranchLogger.d("DEBUG: Retry tracking maps cleaned up")
        } catch (e: Exception) {
            BranchLogger.e("Error cleaning up retry tracking maps: ${e.message}")
        }
    }
    
    /**
     * Print queue state for debugging
     */
    fun printQueue() {
        if (BranchLogger.loggingLevel.level >= BranchLogger.BranchLogLevel.VERBOSE.level) {
            val activeCount = activeRequests.size
            BranchLogger.v("Queue state: ${_queueState.value}, Active requests: $activeCount, Network count: ${networkCount.get()}")
        }
        BranchLogger.d("DEBUG: Queue state: ${_queueState.value}, Queue size: ${getSize()}, Active requests: ${activeRequests.size}, Network count: ${networkCount.get()}")
    }
} 