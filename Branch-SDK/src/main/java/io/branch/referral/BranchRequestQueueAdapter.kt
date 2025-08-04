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
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter constructor called")
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter constructor completed")
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
                    BranchLogger.d("DEBUG: BranchRequestQueueAdapter instance created")
                    newInstance
                }
            }
        }
        
        @JvmStatic
        fun shutDown() {
            BranchLogger.d("DEBUG: BranchRequestQueueAdapter.shutDown called")
            INSTANCE?.get()?.let { instance ->
                instance.shutdown()
                INSTANCE = null
            }
            BranchLogger.d("DEBUG: BranchRequestQueueAdapter.shutDown completed")
        }
    }
    
    /**
     * Initialize the adapter and underlying queue
     */
    fun initialize() {
        BranchLogger.v("Initializing BranchRequestQueueAdapter")
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.initialize called")
        newQueue.initialize()
    }
    
    /**
     * Handle new request - bridge between old callback API and new coroutines API
     */
    fun handleNewRequest(request: ServerRequest) {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.handleNewRequest called for: ${request::class.simpleName}")
        
        // Check if tracking is disabled first (same as original logic)
        if (Branch.init().trackingController.isTrackingDisabled && !request.prepareExecuteWithoutTracking()) {
            val errMsg = "Requested operation cannot be completed since tracking is disabled [${request.requestPath_.getPath()}]"
            BranchLogger.d(errMsg)
            request.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, errMsg)
            return
        }
        
        // Enhanced session validation with fallback to legacy system
        val needsSession = requestNeedsSession(request)
        val canPerformOperations = Branch.init().canPerformOperations()
        val legacyInitialized = Branch.init().initState == Branch.SESSION_STATE.INITIALISED
        val hasValidSession = try {
            Branch.init().hasActiveSession() && 
            !Branch.init().prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)
        } catch (e: Exception) {
            // Fallback if session state is not accessible
            !Branch.init().prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)
        }
        
        BranchLogger.d("DEBUG: Request needs session: $needsSession, can perform operations: $canPerformOperations, legacy initialized: $legacyInitialized, hasValidSession: $hasValidSession")
        
        if (!canPerformOperations && !legacyInitialized && 
            request !is ServerRequestInitSession && needsSession) {
            BranchLogger.d("handleNewRequest $request needs a session")
            
            // Additional check to avoid adding SDK_INIT_WAIT_LOCK if session is actually valid
            val sessionId = Branch.init().prefHelper_.getSessionID()
            val deviceToken = Branch.init().prefHelper_.getRandomizedDeviceToken()
            val actuallyHasSession = !sessionId.equals(PrefHelper.NO_STRING_VALUE) && 
                                   !deviceToken.equals(PrefHelper.NO_STRING_VALUE)
            
            if (actuallyHasSession) {
                BranchLogger.d("DEBUG: Session data is actually valid, not adding SDK_INIT_WAIT_LOCK")
                // Don't add wait lock since session is actually ready
            }
            // If session appears stuck without a valid session, try to allow it to proceed
            else if (!hasValidSession && !legacyInitialized) {
                BranchLogger.d("DEBUG: Session appears stuck without valid session, attempting to reset")
                // Don't add wait lock, let the request proceed and it will trigger proper initialization
            } else {
                BranchLogger.d("DEBUG: Adding SDK_INIT_WAIT_LOCK for request waiting on session")
                request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
            }
        }
        
        // Ensure queue is initialized before processing requests
        if (newQueue.queueState.value == BranchRequestQueue.QueueState.IDLE) {
            BranchLogger.v("Queue not initialized, initializing now")
            BranchLogger.d("DEBUG: Queue was IDLE, initializing now")
            newQueue.initialize()
        }
        
        // Enqueue synchronously - BranchRequestQueue.enqueue is now synchronous
        try {
            BranchLogger.d("DEBUG: Enqueuing request: ${request::class.simpleName}")
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
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.getSize called")
        val result = newQueue.getSize()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.getSize result: $result")
        return result
    }
    fun hasUser(): Boolean {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.hasUser called")
        val result = newQueue.hasUser()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.hasUser result: $result")
        return result
    }
    fun peek(): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.peek called")
        val result = newQueue.peek()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.peek result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun peekAt(index: Int): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.peekAt called for index: $index")
        val result = newQueue.peekAt(index)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.peekAt result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun insert(request: ServerRequest, index: Int) {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.insert called for: ${request::class.simpleName} at index: $index")
        newQueue.insert(request, index)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.insert completed")
    }
    fun removeAt(index: Int): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.removeAt called for index: $index")
        val result = newQueue.removeAt(index)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.removeAt result: ${result?.javaClass?.simpleName}")
        return result
    }
    fun remove(request: ServerRequest?): Boolean {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.remove called for: ${request?.javaClass?.simpleName}")
        val result = newQueue.remove(request)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.remove result: $result")
        return result
    }
    fun insertRequestAtFront(request: ServerRequest) {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.insertRequestAtFront called for: ${request::class.simpleName}")
        newQueue.insertRequestAtFront(request)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.insertRequestAtFront completed")
    }
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.unlockProcessWait called for lock: $lock")
        newQueue.unlockProcessWait(lock)
    }
    fun updateAllRequestsInQueue() {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.updateAllRequestsInQueue called")
        newQueue.updateAllRequestsInQueue()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.updateAllRequestsInQueue completed")
    }
    fun canClearInitData(): Boolean {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.canClearInitData called")
        val result = newQueue.canClearInitData()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.canClearInitData result: $result")
        return result
    }
    fun postInitClear() {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.postInitClear called")
        adapterScope.launch {
            newQueue.postInitClear()
            BranchLogger.d("DEBUG: BranchRequestQueueAdapter.postInitClear completed")
        }
    }
    
    /**
     * Get self init request (matches original API)
     */
    fun getSelfInitRequest(): ServerRequest? {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.getSelfInitRequest called")
        val result = newQueue.getSelfInitRequest()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.getSelfInitRequest result: ${result?.javaClass?.simpleName}")
        return result
    }
    
    /**
     * Instrumentation and debugging
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.addExtraInstrumentationData called - key: $key, value: $value")
        newQueue.addExtraInstrumentationData(key, value)
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.addExtraInstrumentationData completed")
    }
    fun printQueue() {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.printQueue called")
        newQueue.printQueue()
    }
    fun clear() {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.clear called")
        adapterScope.launch {
            newQueue.clear()
            BranchLogger.d("DEBUG: BranchRequestQueueAdapter.clear completed")
        }
    }
    
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        val result = when (request) {
            is ServerRequestInitSession -> false
            is ServerRequestCreateUrl -> false
            else -> true
        }
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.requestNeedsSession for ${request::class.simpleName} - result: $result")
        return result
    }
    
    /**
     * Shutdown the adapter and underlying queue
     */
    fun shutdown() {
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.shutdown called")
        adapterScope.cancel()
        newQueue.shutdown()
        BranchLogger.d("DEBUG: BranchRequestQueueAdapter.shutdown completed")
    }
} 