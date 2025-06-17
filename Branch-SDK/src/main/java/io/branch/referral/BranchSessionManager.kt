package io.branch.referral

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kotlin extension class to handle Branch SDK session state management using StateFlow
 * This class works alongside the existing Branch.java implementation
 */
class BranchSessionManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: BranchSessionManager? = null
        
        fun getInstance(): BranchSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BranchSessionManager().also { INSTANCE = it }
            }
        }
        
        fun shutDown() {
            INSTANCE = null
        }
    }

    // StateFlow for session state
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.UNINITIALIZED)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // List of session state listeners
    private val sessionStateListeners = mutableListOf<BranchSessionStateListener>()

    /**
     * Add a listener for session state changes
     * @param listener The listener to add
     */
    fun addSessionStateListener(listener: BranchSessionStateListener) {
        sessionStateListeners.add(listener)
        // Immediately notify the new listener of current state
        listener.onSessionStateChanged(_sessionState.value)
    }

    /**
     * Remove a session state listener
     * @param listener The listener to remove
     */
    fun removeSessionStateListener(listener: BranchSessionStateListener) {
        sessionStateListeners.remove(listener)
    }

    /**
     * Get the current session state
     */
    fun getSessionState(): SessionState = _sessionState.value

    /**
     * Set the session state and notify listeners
     * @param newState The new session state
     */
    fun setSessionState(newState: SessionState) {
        _sessionState.value = newState
        notifySessionStateListeners()
    }

    /**
     * Notify all session state listeners of a state change
     */
    private fun notifySessionStateListeners() {
        val currentState = _sessionState.value
        sessionStateListeners.forEach { listener ->
            try {
                listener.onSessionStateChanged(currentState)
            } catch (e: Exception) {
                BranchLogger.e("Error notifying session state listener: ${e.message}")
            }
        }
    }

    /**
     * Update session state based on Branch.java state
     * This method should be called whenever the Branch.java state changes
     */
    fun updateFromBranchState(branch: Branch) {
        when (branch.getInitState()) {
            Branch.SESSION_STATE.INITIALISED -> setSessionState(SessionState.INITIALIZED)
            Branch.SESSION_STATE.INITIALISING -> setSessionState(SessionState.INITIALIZING)
            Branch.SESSION_STATE.UNINITIALISED -> setSessionState(SessionState.UNINITIALIZED)
        }
    }
} 