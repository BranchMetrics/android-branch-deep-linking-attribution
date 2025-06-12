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
    
    companion object {
        @Volatile
        private var INSTANCE: BranchRequestQueueAdapter? = null
        
        fun getInstance(context: Context): BranchRequestQueueAdapter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BranchRequestQueueAdapter(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        internal fun shutDown() {
            INSTANCE?.shutdown()
            INSTANCE = null
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
     * Insert request at front - simulate priority queuing
     */
    fun insertRequestAtFront(request: ServerRequest) {
        // For now, just enqueue normally
        // TODO: Implement priority queuing in BranchRequestQueue if needed
        handleNewRequest(request)
    }
    
    /**
     * Unlock process wait locks for all requests
     */
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        // This is handled automatically in the new queue system
        // The new system doesn't use manual locks, so this is a no-op
        BranchLogger.v("unlockProcessWait for $lock - handled automatically in new queue")
    }
    
    /**
     * Process next queue item - trigger processing
     */
    fun processNextQueueItem(callingMethodName: String) {
        BranchLogger.v("processNextQueueItem $callingMethodName - processing is automatic in new queue")
        // Processing is automatic in the new queue system
        // This method exists for compatibility but doesn't need to do anything
    }
    
    /**
     * Get queue size
     */
    fun getSize(): Int = newQueue.getSize()
    
    /**
     * Check if queue has user
     */
    fun hasUser(): Boolean = newQueue.hasUser()
    
    /**
     * Add instrumentation data
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        newQueue.addExtraInstrumentationData(key, value)
    }
    
    /**
     * Clear all pending requests
     */
    fun clear() {
        adapterScope.launch {
            newQueue.clear()
        }
    }
    
    /**
     * Print queue for debugging
     */
    fun printQueue() {
        newQueue.printQueue()
    }
    
    /**
     * Get self init request - for compatibility
     */
    internal fun getSelfInitRequest(): ServerRequestInitSession? {
        return newQueue.getSelfInitRequest()
    }
    
    /**
     * Peek at first request - for compatibility
     */
    fun peek(): ServerRequest? {
        return newQueue.peek()
    }
    
    /**
     * Peek at specific index - for compatibility
     */
    fun peekAt(index: Int): ServerRequest? {
        return newQueue.peekAt(index)
    }
    
    /**
     * Insert request at specific index - for compatibility
     */
    fun insert(request: ServerRequest, index: Int) {
        newQueue.insert(request, index)
    }
    
    /**
     * Remove request at specific index - for compatibility
     */
    fun removeAt(index: Int): ServerRequest? {
        return newQueue.removeAt(index)
    }
    
    /**
     * Remove specific request - for compatibility
     */
    fun remove(request: ServerRequest?): Boolean {
        return newQueue.remove(request)
    }
    
    /**
     * Insert request at front - for compatibility
     */
    fun insertRequestAtFront(request: ServerRequest) {
        newQueue.insertRequestAtFront(request)
    }
    
    /**
     * Unlock process wait - for compatibility
     */
    fun unlockProcessWait(lock: ServerRequest.PROCESS_WAIT_LOCK) {
        newQueue.unlockProcessWait(lock)
    }
    
    /**
     * Update all requests in queue - for compatibility
     */
    fun updateAllRequestsInQueue() {
        newQueue.updateAllRequestsInQueue()
    }
    
    /**
     * Check if init data can be cleared - for compatibility
     */
    fun canClearInitData(): Boolean {
        return newQueue.canClearInitData()
    }
    
    /**
     * Post init clear - for compatibility
     */
    fun postInitClear() {
        newQueue.postInitClear()
    }
    
    /**
     * Check if can clear init data
     */
    fun canClearInitData(): Boolean {
        // Simplified logic for new system
        return true
    }
    
    /**
     * Post init clear - for compatibility
     */
    fun postInitClear() {
        BranchLogger.v("postInitClear - handled automatically in new queue")
    }
    
    /**
     * Private helper methods
     */
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        return when (request) {
            is ServerRequestInitSession -> false
            is ServerRequestCreateUrl -> false
            else -> true
        }
    }
    
    private fun shutdown() {
        adapterScope.cancel("Adapter shutdown")
    }
} 