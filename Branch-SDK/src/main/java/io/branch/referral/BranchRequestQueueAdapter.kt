package io.branch.referral

import android.content.Context
import kotlinx.coroutines.*

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
    
    companion object {
        @Volatile
        private var INSTANCE: BranchRequestQueueAdapter? = null
        
        @JvmStatic
        fun getInstance(context: Context): BranchRequestQueueAdapter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BranchRequestQueueAdapter(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        @JvmStatic
        fun shutDown() {
            INSTANCE?.let {
                it.shutdown()
                INSTANCE = null
            }
        }
    }
    
    /**
     * Handle new request - bridge between old callback API and new coroutines API
     */
    fun handleNewRequest(request: ServerRequest) {
        // Check if tracking is disabled first (same as original logic)
        if (Branch.getInstance().trackingController.isTrackingDisabled && !request.prepareExecuteWithoutTracking()) {
            val errMsg = "Requested operation cannot be completed since tracking is disabled [${request.requestPath_.getPath()}]"
            BranchLogger.d(errMsg)
            request.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, errMsg)
            return
        }
        
        // Handle session requirements (similar to original logic)
        if (Branch.getInstance().initState_ != Branch.SESSION_STATE.INITIALISED && 
            request !is ServerRequestInitSession && 
            requestNeedsSession(request)) {
            BranchLogger.d("handleNewRequest $request needs a session")
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
        }
        
        // Enqueue using coroutines (non-blocking)
        adapterScope.launch {
            try {
                newQueue.enqueue(request)
            } catch (e: Exception) {
                BranchLogger.e("Failed to enqueue request: ${e.message}")
                request.handleFailure(BranchError.ERR_OTHER, "Failed to enqueue request")
            }
        }
    }
    
    /**
     * Process next queue item - trigger processing
     */
    fun processNextQueueItem(callingMethodName: String) {
        BranchLogger.v("processNextQueueItem $callingMethodName - processing is automatic in new queue")
    }
    
    /**
     * Queue operations - delegating to new queue implementation
     */
    fun getSize(): Int = newQueue.getSize()
    fun hasUser(): Boolean = newQueue.hasUser()
    fun peek(): ServerRequest? = newQueue.peek()
    fun peekAt(index: Int): ServerRequest? = newQueue.peekAt(index)
    fun insert(request: ServerRequest, index: Int) = newQueue.insert(request, index)
    fun removeAt(index: Int): ServerRequest? = newQueue.removeAt(index)
    fun remove(request: ServerRequest?): Boolean = newQueue.remove(request)
    fun insertRequestAtFront(request: ServerRequest) = newQueue.insertRequestAtFront(request)
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) = newQueue.unlockProcessWait(lock)
    fun updateAllRequestsInQueue() = newQueue.updateAllRequestsInQueue()
    fun canClearInitData(): Boolean = newQueue.canClearInitData()
    fun postInitClear() = newQueue.postInitClear()
    
    /**
     * Get self init request - for compatibility with Java
     */
    @JvmName("getSelfInitRequest")
    internal fun getSelfInitRequest(): ServerRequestInitSession? = newQueue.getSelfInitRequest()
    
    /**
     * Instrumentation and debugging
     */
    fun addExtraInstrumentationData(key: String, value: String) = newQueue.addExtraInstrumentationData(key, value)
    fun printQueue() = newQueue.printQueue()
    fun clear() = adapterScope.launch { newQueue.clear() }
    
    private fun requestNeedsSession(request: ServerRequest): Boolean = when (request) {
        is ServerRequestInitSession -> false
        is ServerRequestCreateUrl -> false
        else -> true
    }
    
    private fun shutdown() {
        adapterScope.cancel("Adapter shutdown")
        newQueue.shutdown()
    }
} 