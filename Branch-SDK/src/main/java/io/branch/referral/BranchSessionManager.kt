package io.branch.referral

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface to safely expose Branch session states
 */
interface BranchSessionStateProvider {
    fun isInitialized(): Boolean
    fun isInitializing(): Boolean
    fun isUninitialized(): Boolean
}

/**
 * Manages the session state of the Branch SDK.
 * This class serves as a facade for the BranchSessionStateManager, providing a simpler interface
 * for the rest of the SDK to interact with session state.
 */
class BranchSessionManager {
    private val stateManager = BranchSessionStateManager()

    /**
     * Gets the current session state.
     * @return The current BranchSessionState
     */
    fun getSessionState(): BranchSessionState = stateManager.getCurrentState()

    /**
     * Gets the session state as a StateFlow for reactive programming.
     * @return A StateFlow containing the current session state
     */
    val sessionState: StateFlow<BranchSessionState> = stateManager.sessionState

    /**
     * Adds a listener for session state changes.
     * @param listener The listener to add
     */
    fun addSessionStateListener(listener: BranchSessionStateListener) {
        stateManager.addListener(listener)
    }

    /**
     * Removes a listener for session state changes.
     * @param listener The listener to remove
     */
    fun removeSessionStateListener(listener: BranchSessionStateListener) {
        stateManager.removeListener(listener)
    }

    /**
     * Updates the session state based on the current state of the Branch instance.
     * This method ensures that the session state is synchronized with the Branch instance.
     * @param branch The Branch instance to check the state from
     */
    fun updateFromBranchState(branch: Branch) {
        val currentState = stateManager.getCurrentState()
        val branchState = branch.getInitState()

        when {
            branchState == Branch.SESSION_STATE.INITIALISED && currentState !is BranchSessionState.Initialized -> {
                stateManager.transitionToInitialized()
            }
            branchState == Branch.SESSION_STATE.INITIALISING && currentState !is BranchSessionState.Initializing -> {
                stateManager.transitionToInitializing()
            }
            branchState == Branch.SESSION_STATE.UNINITIALISED && currentState !is BranchSessionState.Uninitialized -> {
                stateManager.transitionToUninitialized()
            }
        }
    }

    /**
     * Gets debug information about the current state.
     * @return A string containing debug information
     */
    fun getDebugInfo(): String = stateManager.getDebugInfo()
} 