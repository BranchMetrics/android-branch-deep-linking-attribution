package io.branch.referral

/**
 * Interface for observing Branch SDK session state changes.
 * Provides deterministic state observation for SDK clients.
 */
interface BranchSessionStateListener {
    /**
     * Called when the Branch SDK session state changes.
     * This method is guaranteed to be called on the main thread.
     * 
     * @param previousState The previous session state (null for the first notification)
     * @param currentState The new current session state
     */
    fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState)
}

/**
 * Functional interface for simplified session state observation.
 * Use this when you only need to observe the current state without previous state context.
 */
fun interface SimpleBranchSessionStateListener {
    /**
     * Called when the Branch SDK session state changes.
     * This method is guaranteed to be called on the main thread.
     * 
     * @param state The new current session state
     */
    fun onStateChanged(state: BranchSessionState)
}

/**
 * Extension function to convert SimpleBranchSessionStateListener to BranchSessionStateListener
 */
fun SimpleBranchSessionStateListener.toBranchSessionStateListener(): BranchSessionStateListener {
    return object : BranchSessionStateListener {
        override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
            onStateChanged(currentState)
        }
    }
} 