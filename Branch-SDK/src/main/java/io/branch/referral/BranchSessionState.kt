package io.branch.referral

/**
 * Represents the current state of the Branch SDK session.
 * This sealed class provides type-safe state management and enables deterministic state observation.
 */
sealed class BranchSessionState {
    /**
     * SDK has not been initialized yet
     */
    object Uninitialized : BranchSessionState()
    
    /**
     * SDK initialization is in progress
     */
    object Initializing : BranchSessionState()
    
    /**
     * SDK has been successfully initialized and is ready for use
     */
    object Initialized : BranchSessionState()
    
    /**
     * SDK initialization failed with an error
     * @param error The error that caused the initialization failure
     */
    data class Failed(val error: BranchError) : BranchSessionState()
    
    /**
     * SDK is in the process of resetting/clearing session
     */
    object Resetting : BranchSessionState()
    
    override fun toString(): String = when (this) {
        is Uninitialized -> "Uninitialized"
        is Initializing -> "Initializing"
        is Initialized -> "Initialized"
        is Failed -> "Failed(${error.message})"
        is Resetting -> "Resetting"
    }
    
    /**
     * Checks if the current state allows new operations
     */
    fun canPerformOperations(): Boolean = when (this) {
        is Initialized -> true
        else -> false
    }
    
    /**
     * Checks if the current state indicates an active session
     */
    fun hasActiveSession(): Boolean = when (this) {
        is Initialized -> true
        else -> false
    }
    
    /**
     * Checks if the current state indicates a terminal error
     */
    fun isErrorState(): Boolean = when (this) {
        is Failed -> true
        else -> false
    }
} 