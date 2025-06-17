package io.branch.referral

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe session state manager using StateFlow.
 * Replaces the manual lock-based SESSION_STATE system with a deterministic, observable state management.
 */
class BranchSessionStateManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: BranchSessionStateManager? = null
        
        fun getInstance(): BranchSessionStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BranchSessionStateManager().also { INSTANCE = it }
            }
        }
        
        @JvmStatic
        fun resetInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
    
    // Core StateFlow for session state - thread-safe and observable
    private val _sessionState = MutableStateFlow<BranchSessionState>(BranchSessionState.Uninitialized)
    val sessionState: StateFlow<BranchSessionState> = _sessionState.asStateFlow()
    
    // Thread-safe listener management
    private val listeners = CopyOnWriteArrayList<BranchSessionStateListener>()
    
    // Previous state tracking for listener notifications
    private val previousState = AtomicReference<BranchSessionState?>(null)
    
    // Main thread handler for listener notifications
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Get the current session state synchronously
     */
    fun getCurrentState(): BranchSessionState = _sessionState.value
    
    /**
     * Update the session state in a thread-safe manner
     * @param newState The new state to transition to
     * @return true if the state was updated, false if the transition is invalid
     */
    fun updateState(newState: BranchSessionState): Boolean {
        val currentState = _sessionState.value
        
        // Validate state transition
        if (!isValidTransition(currentState, newState)) {
            BranchLogger.w("Invalid state transition from $currentState to $newState")
            return false
        }
        
        BranchLogger.v("Session state transition: $currentState -> $newState")
        
        // Update the state atomically
        val oldState = previousState.getAndSet(currentState)
        _sessionState.value = newState
        
        // Notify listeners on main thread
        notifyListeners(currentState, newState)
        
        return true
    }
    
    /**
     * Force update the state without validation (use with caution)
     */
    internal fun forceUpdateState(newState: BranchSessionState) {
        val currentState = _sessionState.value
        BranchLogger.v("Force session state transition: $currentState -> $newState")
        
        val oldState = previousState.getAndSet(currentState)
        _sessionState.value = newState
        
        notifyListeners(currentState, newState)
    }
    
    /**
     * Add a session state listener
     * @param listener The listener to add
     * @param notifyImmediately Whether to immediately notify with current state
     */
    fun addListener(listener: BranchSessionStateListener, notifyImmediately: Boolean = true) {
        listeners.add(listener)
        
        if (notifyImmediately) {
            val current = getCurrentState()
            mainHandler.post {
                try {
                    listener.onSessionStateChanged(null, current)
                } catch (e: Exception) {
                    BranchLogger.e("Error notifying session state listener: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Add a simple session state listener
     */
    fun addListener(listener: SimpleBranchSessionStateListener, notifyImmediately: Boolean = true) {
        addListener(listener.toBranchSessionStateListener(), notifyImmediately)
    }
    
    /**
     * Remove a session state listener
     */
    fun removeListener(listener: BranchSessionStateListener) {
        listeners.remove(listener)
    }
    
    /**
     * Remove all listeners
     */
    fun clearListeners() {
        listeners.clear()
    }
    
    /**
     * Get the number of registered listeners
     */
    fun getListenerCount(): Int = listeners.size
    
    /**
     * Check if the current state allows operations
     */
    fun canPerformOperations(): Boolean = getCurrentState().canPerformOperations()
    
    /**
     * Check if there's an active session
     */
    fun hasActiveSession(): Boolean = getCurrentState().hasActiveSession()
    
    /**
     * Check if the current state is an error state
     */
    fun isErrorState(): Boolean = getCurrentState().isErrorState()
    
    /**
     * Reset the session state to Uninitialized
     */
    fun reset() {
        updateState(BranchSessionState.Resetting)
        // Small delay to ensure any pending operations see the resetting state
        mainHandler.postDelayed({
            forceUpdateState(BranchSessionState.Uninitialized)
        }, 10)
    }
    
    /**
     * Initialize the session
     */
    fun initialize(): Boolean {
        return updateState(BranchSessionState.Initializing)
    }
    
    /**
     * Mark initialization as completed successfully
     */
    fun initializeComplete(): Boolean {
        return updateState(BranchSessionState.Initialized)
    }
    
    /**
     * Mark initialization as failed
     */
    fun initializeFailed(error: BranchError): Boolean {
        return updateState(BranchSessionState.Failed(error))
    }
    
    /**
     * Validate state transitions to prevent invalid state changes
     */
    private fun isValidTransition(from: BranchSessionState, to: BranchSessionState): Boolean {
        return when (from) {
            is BranchSessionState.Uninitialized -> {
                to is BranchSessionState.Initializing || to is BranchSessionState.Resetting
            }
            is BranchSessionState.Initializing -> {
                to is BranchSessionState.Initialized || 
                to is BranchSessionState.Failed || 
                to is BranchSessionState.Resetting
            }
            is BranchSessionState.Initialized -> {
                to is BranchSessionState.Resetting || 
                to is BranchSessionState.Initializing // Allow re-initialization
            }
            is BranchSessionState.Failed -> {
                to is BranchSessionState.Initializing || // Allow retry
                to is BranchSessionState.Resetting
            }
            is BranchSessionState.Resetting -> {
                to is BranchSessionState.Uninitialized ||
                to is BranchSessionState.Initializing
            }
        }
    }
    
    /**
     * Notify all listeners of state change on main thread
     */
    private fun notifyListeners(previousState: BranchSessionState, currentState: BranchSessionState) {
        if (listeners.isEmpty()) return
        
        mainHandler.post {
            // Create a snapshot of listeners to avoid ConcurrentModificationException
            val listenerSnapshot = listeners.toList()
            
            for (listener in listenerSnapshot) {
                try {
                    listener.onSessionStateChanged(previousState, currentState)
                } catch (e: Exception) {
                    BranchLogger.e("Error notifying session state listener: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get debug information about the current state
     */
    fun getDebugInfo(): String {
        val current = getCurrentState()
        val prev = previousState.get()
        return buildString {
            append("Current State: $current\n")
            append("Previous State: $prev\n")
            append("Listener Count: ${listeners.size}\n")
            append("Can Perform Operations: ${current.canPerformOperations()}\n")
            append("Has Active Session: ${current.hasActiveSession()}\n")
            append("Is Error State: ${current.isErrorState()}")
        }
    }
} 