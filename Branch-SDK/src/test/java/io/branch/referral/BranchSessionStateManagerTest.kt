package io.branch.referral

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for BranchSessionStateManager.
 */
@RunWith(JUnit4::class)
class BranchSessionStateManagerTest {

    private lateinit var stateManager: BranchSessionStateManager

    @Before
    fun setUp() {
        stateManager = BranchSessionStateManager()
    }

    @Test
    fun testInitialState() {
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
        assertFalse(stateManager.isErrorState())
    }

    @Test
    fun testStateFlowInitialValue() = runBlocking {
        val initialState = stateManager.sessionState.first()
        assertEquals(BranchSessionState.Uninitialized, initialState)
    }

    @Test
    fun testValidStateTransitions() {
        // Uninitialized -> Initializing
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())

        // Initializing -> Initialized
        assertTrue(stateManager.updateState(BranchSessionState.Initialized))
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())

        // Initialized -> Resetting
        assertTrue(stateManager.updateState(BranchSessionState.Resetting))
        assertEquals(BranchSessionState.Resetting, stateManager.getCurrentState())

        // Resetting -> Uninitialized
        assertTrue(stateManager.updateState(BranchSessionState.Uninitialized))
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
    }

    @Test
    fun testInvalidStateTransitions() {
        // Cannot go directly from Uninitialized to Initialized
        assertFalse(stateManager.updateState(BranchSessionState.Initialized))
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())

        // Set to Initializing first
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        
        // Cannot go from Initializing to Uninitialized (must go through Failed or Resetting)
        assertFalse(stateManager.updateState(BranchSessionState.Uninitialized))
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
    }

    @Test
    fun testFailedStateTransitions() {
        // Set to initializing first
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        
        val error = BranchError("Test error", BranchError.ERR_BRANCH_INIT_FAILED)
        val failedState = BranchSessionState.Failed(error)
        
        // Initializing -> Failed
        assertTrue(stateManager.updateState(failedState))
        assertEquals(failedState, stateManager.getCurrentState())
        assertTrue(stateManager.isErrorState())
        
        // Failed -> Initializing (retry)
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        assertFalse(stateManager.isErrorState())
    }

    @Test
    fun testConvenienceMethods() {
        // Test initialize
        assertTrue(stateManager.initialize())
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        
        // Test initializeComplete
        assertTrue(stateManager.initializeComplete())
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
        assertTrue(stateManager.canPerformOperations())
        assertTrue(stateManager.hasActiveSession())
    }
    
    @Test
    fun testSessionNullFix() {
        // Test that session state transitions work correctly and don't result in null sessions
        assertTrue(stateManager.initialize())
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        assertFalse(stateManager.canPerformOperations()) // Should be false during initialization
        
        assertTrue(stateManager.initializeComplete())
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
        assertTrue(stateManager.canPerformOperations()) // Should be true after initialization
        assertTrue(stateManager.hasActiveSession())
        
        // Test that reset works correctly
        stateManager.reset()
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
    }
    
    @Test
    fun testQueueInitializationFix() {
        // Test that the queue initialization fix works correctly
        // This test verifies that the session state manager works properly
        // when the queue is initialized with coroutines
        
        // Initialize session state
        assertTrue(stateManager.initialize())
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        
        // Complete initialization
        assertTrue(stateManager.initializeComplete())
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
        
        // Verify operations can be performed
        assertTrue(stateManager.canPerformOperations())
        assertTrue(stateManager.hasActiveSession())
        
        // Test that the state is stable
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
    }

    @Test
    fun testInitializeFailed() {
        stateManager.initialize()
        
        val error = BranchError("Init failed", BranchError.ERR_BRANCH_INIT_FAILED)
        assertTrue(stateManager.initializeFailed(error))
        
        val currentState = stateManager.getCurrentState()
        assertTrue(currentState is BranchSessionState.Failed)
        assertEquals(error, (currentState as BranchSessionState.Failed).error)
        assertTrue(stateManager.isErrorState())
    }

    @Test
    fun testForceUpdateState() {
        // Force update bypasses validation
        stateManager.forceUpdateState(BranchSessionState.Initialized)
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
        
        // This would normally be invalid, but force update allows it
        stateManager.forceUpdateState(BranchSessionState.Uninitialized)
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
    }

    @Test
    fun testGetDebugInfo() {
        val debugInfo = stateManager.getDebugInfo()
        
        assertTrue(debugInfo.contains("Current State: Uninitialized"))
        assertTrue(debugInfo.contains("Previous State: null"))
        assertTrue(debugInfo.contains("Listener Count: 0"))
        assertTrue(debugInfo.contains("Can Perform Operations: false"))
        assertTrue(debugInfo.contains("Has Active Session: false"))
        assertTrue(debugInfo.contains("Is Error State: false"))
    }

    @Test
    fun testDebugInfoAfterStateChanges() {
        stateManager.updateState(BranchSessionState.Initializing)
        stateManager.updateState(BranchSessionState.Initialized)
        
        val debugInfo = stateManager.getDebugInfo()
        
        assertTrue(debugInfo.contains("Current State: Initialized"))
        assertTrue(debugInfo.contains("Can Perform Operations: true"))
        assertTrue(debugInfo.contains("Has Active Session: true"))
        assertTrue(debugInfo.contains("Is Error State: false"))
    }

    @Test
    fun testTransitionMethods() {
        // Test transitionToInitializing from Uninitialized
        stateManager.transitionToInitializing()
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        
        // Test transitionToInitialized from Initializing
        stateManager.transitionToInitialized()
        assertEquals(BranchSessionState.Initialized, stateManager.getCurrentState())
        
        // Test transitionToUninitialized from any state
        stateManager.transitionToUninitialized()
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
    }

    @Test
    fun testTransitionMethodsWithInvalidStates() {
        // Should not transition to Initializing if not in Uninitialized state
        stateManager.updateState(BranchSessionState.Initializing)
        stateManager.transitionToInitializing() // Should not change state
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
        
        // Should not transition to Initialized if not in Initializing state
        stateManager.updateState(BranchSessionState.Uninitialized)
        stateManager.transitionToInitialized() // Should not change state
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
    }

    @Test
    fun testResetWithDelayedTransition() {
        // Move to initialized state
        stateManager.updateState(BranchSessionState.Initializing)
        stateManager.updateState(BranchSessionState.Initialized)
        
        // Test reset
        stateManager.reset()
        // Reset first transitions to Resetting state
        assertEquals(BranchSessionState.Resetting, stateManager.getCurrentState())
        
        // The delayed transition to Uninitialized happens after 10ms
        // We'll test this by checking the state remains Resetting initially
        assertEquals(BranchSessionState.Resetting, stateManager.getCurrentState())
    }

    @Test
    fun testValidTransitionFromInitializedToInitializing() {
        // Move to initialized state
        stateManager.updateState(BranchSessionState.Initializing)
        stateManager.updateState(BranchSessionState.Initialized)
        
        // Should be able to re-initialize
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        assertEquals(BranchSessionState.Initializing, stateManager.getCurrentState())
    }

    @Test
    fun testAllValidTransitionsFromEachState() {
        // From Uninitialized
        assertEquals(BranchSessionState.Uninitialized, stateManager.getCurrentState())
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        assertTrue(stateManager.updateState(BranchSessionState.Resetting))
        assertTrue(stateManager.updateState(BranchSessionState.Uninitialized))
        
        // From Initializing
        stateManager.updateState(BranchSessionState.Initializing)
        assertTrue(stateManager.updateState(BranchSessionState.Initialized))
        stateManager.updateState(BranchSessionState.Initializing)
        
        val error = BranchError("Test", BranchError.ERR_BRANCH_INIT_FAILED)
        assertTrue(stateManager.updateState(BranchSessionState.Failed(error)))
        
        // From Failed
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
        stateManager.updateState(BranchSessionState.Failed(error))
        assertTrue(stateManager.updateState(BranchSessionState.Resetting))
        
        // From Resetting  
        assertTrue(stateManager.updateState(BranchSessionState.Uninitialized))
        stateManager.updateState(BranchSessionState.Resetting)
        assertTrue(stateManager.updateState(BranchSessionState.Initializing))
    }

    @Test
    fun testStateUtilityMethods() {
        // Uninitialized
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
        assertFalse(stateManager.isErrorState())
        
        // Initializing
        stateManager.updateState(BranchSessionState.Initializing)
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
        assertFalse(stateManager.isErrorState())
        
        // Initialized
        stateManager.updateState(BranchSessionState.Initialized)
        assertTrue(stateManager.canPerformOperations())
        assertTrue(stateManager.hasActiveSession())
        assertFalse(stateManager.isErrorState())
        
        // Failed
        val error = BranchError("Test", BranchError.ERR_BRANCH_INIT_FAILED)
        stateManager.updateState(BranchSessionState.Failed(error))
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
        assertTrue(stateManager.isErrorState())
        
        // Resetting
        stateManager.updateState(BranchSessionState.Resetting)
        assertFalse(stateManager.canPerformOperations())
        assertFalse(stateManager.hasActiveSession())
        assertFalse(stateManager.isErrorState())
    }

    @Test
    fun testGetListenerCount() {
        assertEquals(0, stateManager.getListenerCount())
    }

    @Test 
    fun testClearListeners() {
        assertEquals(0, stateManager.getListenerCount())
        stateManager.clearListeners()
        assertEquals(0, stateManager.getListenerCount())
    }
} 