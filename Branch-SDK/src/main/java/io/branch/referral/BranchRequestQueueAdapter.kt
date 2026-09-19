package io.branch.referral

import android.content.Context
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * Adapter class to integrate the new BranchRequestQueue with existing ServerRequestQueue API
 * This allows for gradual migration from the old system to the new coroutines-based system
 */
class BranchRequestQueueAdapter private constructor(context: Context) {
    
    private val newQueue = BranchRequestQueue.getInstance(context)
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Make instrumentationExtraData public and match original name with underscore
    @JvmField
    val instrumentationExtraData_ = newQueue.instrumentationExtraData
    
    init {
        BranchLogger.v("BranchRequestQueueAdapter constructor called")
        BranchLogger.v("BranchRequestQueueAdapter constructor completed")
    }
    
    companion object {
        // Use WeakReference to prevent memory leaks
        @Volatile
        private var INSTANCE: WeakReference<BranchRequestQueueAdapter>? = null
        
        @JvmStatic
        fun getInstance(context: Context): BranchRequestQueueAdapter {
            // Check if we have a valid instance
            INSTANCE?.get()?.let { return it }
            
            // Create new instance with proper synchronization
            return synchronized(this) {
                // Double-check after acquiring lock
                INSTANCE?.get() ?: run {
                    val newInstance = BranchRequestQueueAdapter(context)
                    INSTANCE = WeakReference(newInstance)
                    BranchLogger.v("BranchRequestQueueAdapter instance created")
                    newInstance
                }
            }
        }
        
        @JvmStatic
        fun shutDown() {
            BranchLogger.v("BranchRequestQueueAdapter.shutDown called")
            INSTANCE?.get()?.let { instance ->
                instance.shutdown()
                INSTANCE = null
            }
            BranchLogger.v("BranchRequestQueueAdapter.shutDown completed")
        }
    }
    
    /**
     * Initialize the adapter and underlying queue
     */
    fun initialize() {
        BranchLogger.v("Initializing BranchRequestQueueAdapter")
        BranchLogger.v("BranchRequestQueueAdapter.initialize called")
        newQueue.initialize()
    }
    
    /**
     * Handle new request - bridge between old callback API and new coroutines API
     */
    fun handleNewRequest(request: ServerRequest) {
        BranchLogger.v("BranchRequestQueueAdapter.handleNewRequest called for: ${request::class.simpleName}")
        
        // Check if tracking is disabled first (same as original logic)
        if (Branch.getInstance().trackingController.isTrackingDisabled && !request.prepareExecuteWithoutTracking()) {
            val errMsg = "Requested operation cannot be completed since tracking is disabled [${request.requestPath_.getPath()}]"
            BranchLogger.d(errMsg)
            request.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, errMsg)
            return
        }
        
        // Enhanced session validation with fallback to legacy system
        val needsSession = requestNeedsSession(request)
        val canPerformOperations = Branch.getInstance().canPerformOperations()
        val legacyInitialized = Branch.getInstance().initState is BranchSessionState.Initialized
        val hasValidSession = try {
            Branch.getInstance().hasActiveSession() &&
            !Branch.getInstance().prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)
        } catch (e: Exception) {
            // Fallback if session state is not accessible
            !Branch.getInstance().prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)
        }
        
        BranchLogger.v("Request needs session: $needsSession, can perform operations: $canPerformOperations, legacy initialized: $legacyInitialized, hasValidSession: $hasValidSession")
        
        if (!canPerformOperations && !legacyInitialized && 
            request !is ServerRequestInitSession && needsSession) {
            BranchLogger.d("handleNewRequest $request needs a session")
            
            // Additional check to avoid adding SDK_INIT_WAIT_LOCK if session is actually valid
            val sessionId = Branch.getInstance().prefHelper_.getSessionID()
            val deviceToken = Branch.getInstance().prefHelper_.getRandomizedDeviceToken()
            val actuallyHasSession = !sessionId.equals(PrefHelper.NO_STRING_VALUE) && 
                                   !deviceToken.equals(PrefHelper.NO_STRING_VALUE)
            
            if (actuallyHasSession) {
                BranchLogger.v("Session data is actually valid, not adding SDK_INIT_WAIT_LOCK")
                // Don't add wait lock since session is actually ready
            }
            // If session appears stuck without a valid session, try to allow it to proceed
            else if (!hasValidSession && !legacyInitialized) {
                BranchLogger.v("Session appears stuck without valid session, attempting to reset")
                // Don't add wait lock, let the request proceed and it will trigger proper initialization
            } else {
                BranchLogger.v("Adding SDK_INIT_WAIT_LOCK for request waiting on session")
                request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
            }
        }
        
        // Ensure queue is initialized before processing requests
        if (newQueue.queueState.value == BranchRequestQueue.QueueState.IDLE) {
            BranchLogger.v("Queue not initialized, initializing now")
            BranchLogger.v("Queue was IDLE, initializing now")
            newQueue.initialize()
        }
        
        // Enqueue synchronously - BranchRequestQueue.enqueue is now synchronous
        try {
            BranchLogger.v("Enqueuing request: ${request::class.simpleName}")
            newQueue.enqueue(request)
        } catch (e: Exception) {
            BranchLogger.e("Failed to enqueue request: ${e.message}")
            request.handleFailure(BranchError.ERR_OTHER, "Failed to enqueue request")
        }
    }
    
    /**
     * Queue operations - delegating to new queue implementation
     */
    fun getSize(): Int {
        BranchLogger.v("BranchRequestQueueAdapter.getSize called")
        val result = newQueue.getSize()
        BranchLogger.v("BranchRequestQueueAdapter.getSize result: $result")
        return result
    }
    fun hasUser(): Boolean {
        BranchLogger.v("BranchRequestQueueAdapter.hasUser called")
        val result = newQueue.hasUser()
        BranchLogger.v("BranchRequestQueueAdapter.hasUser result: $result")
        return result
    }
    fun containsInstallOrOpen(): Boolean {
        BranchLogger.v("BranchRequestQueueAdapter.containsInstallOrOpen called")
        val result = newQueue.containsInstallOrOpen()
        BranchLogger.v("BranchRequestQueueAdapter.containsInstallOrOpen result: $result")
        return result
    }
    fun peek(): ServerRequest? {
        BranchLogger.v("BranchRequestQueueAdapter.peek called")
        val result = newQueue.peek()
        BranchLogger.v("BranchRequestQueueAdapter.peek result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun peekAt(index: Int): ServerRequest? {
        BranchLogger.v("BranchRequestQueueAdapter.peekAt called for index: $index")
        val result = newQueue.peekAt(index)
        BranchLogger.v("BranchRequestQueueAdapter.peekAt result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun insert(request: ServerRequest, index: Int) {
        BranchLogger.v("BranchRequestQueueAdapter.insert called for: ${request::class.simpleName} at index: $index")
        newQueue.insert(request, index)
        BranchLogger.v("BranchRequestQueueAdapter.insert completed")
    }
    fun removeAt(index: Int): ServerRequest? {
        BranchLogger.v("BranchRequestQueueAdapter.removeAt called for index: $index")
        val result = newQueue.removeAt(index)
        BranchLogger.v("BranchRequestQueueAdapter.removeAt result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun remove(request: ServerRequest?): Boolean {
        BranchLogger.v("BranchRequestQueueAdapter.remove called for: ${request?.javaClass?.simpleName}")
        val result = newQueue.remove(request)
        BranchLogger.v("BranchRequestQueueAdapter.remove result: $result")
        return result
    }
    fun insertRequestAtFront(request: ServerRequest) {
        BranchLogger.v("BranchRequestQueueAdapter.insertRequestAtFront called for: ${request::class.simpleName}")
        newQueue.insertRequestAtFront(request)
        BranchLogger.v("BranchRequestQueueAdapter.insertRequestAtFront completed")
    }
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        BranchLogger.v("BranchRequestQueueAdapter.unlockProcessWait called for lock: $lock")
        newQueue.unlockProcessWait(lock)
    }
    fun updateAllRequestsInQueue() {
        BranchLogger.v("BranchRequestQueueAdapter.updateAllRequestsInQueue called")
        newQueue.updateAllRequestsInQueue()
        BranchLogger.v("BranchRequestQueueAdapter.updateAllRequestsInQueue completed")
    }
    fun postInitClear() {
        BranchLogger.v("BranchRequestQueueAdapter.postInitClear called")
        adapterScope.launch {
            newQueue.clearDeepLinkStorage()
            BranchLogger.v("BranchRequestQueueAdapter.postInitClear completed")
        }
    }
    
    /**
     * Get self init request (matches original API)
     */
    fun getSelfInitRequest(): ServerRequest? {
        BranchLogger.v("BranchRequestQueueAdapter.getSelfInitRequest called")
        val result = newQueue.getSelfInitRequest()
        BranchLogger.v("BranchRequestQueueAdapter.getSelfInitRequest result: ${result?.javaClass?.simpleName}")
        return result
    }
    
    /**
     * Whether init data can be cleared (delegates to the modern queue). Matches the legacy
     * ServerRequestQueue API so callers and tests written against it keep compiling.
     */
    fun canClearInitData(): Boolean {
        val result = newQueue.canClearInitData()
        return result
    }

    /**
     * Instrumentation and debugging
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        BranchLogger.v("BranchRequestQueueAdapter.addExtraInstrumentationData called - key: $key, value: $value")
        newQueue.addExtraInstrumentationData(key, value)
        BranchLogger.v("BranchRequestQueueAdapter.addExtraInstrumentationData completed")
    }
    fun printQueue() {
        BranchLogger.v("BranchRequestQueueAdapter.printQueue called")
        newQueue.printQueue()
    }
    fun clear() {
        BranchLogger.v("BranchRequestQueueAdapter.clear called")
        adapterScope.launch {
            newQueue.clear()
            BranchLogger.v("BranchRequestQueueAdapter.clear completed")
        }
    }
    
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        val result = when (request) {
            is ServerRequestInitSession -> false
            is ServerRequestCreateUrl -> false
            is QueueOperationLogout -> false
            is QueueOperationSetIdentity -> false
            else -> true
        }
        BranchLogger.v("BranchRequestQueueAdapter.requestNeedsSession for ${request::class.simpleName} - result: $result")
        return result
    }
    
    /**
     * Shutdown the adapter and underlying queue
     */
    fun shutdown() {
        BranchLogger.v("BranchRequestQueueAdapter.shutdown called")
        adapterScope.cancel()
        newQueue.shutdown()
        BranchLogger.v("BranchRequestQueueAdapter.shutdown completed")
    }
} 